package com.cinema.cinema_backend.service;

import com.cinema.cinema_backend.dto.ReservationRequest;
import com.cinema.cinema_backend.dto.ReservationResponse;
import com.cinema.cinema_backend.dto.TicketResponse;
import com.cinema.cinema_backend.entity.Member;
import com.cinema.cinema_backend.entity.Reservation;
import com.cinema.cinema_backend.entity.Screening;
import com.cinema.cinema_backend.entity.Seat;
import com.cinema.cinema_backend.entity.Ticket;
import com.cinema.cinema_backend.exception.CinemaException;
import com.cinema.cinema_backend.exception.ErrorCode;
import com.cinema.cinema_backend.exception.SeatAlreadyReservedException;
import com.cinema.cinema_backend.repository.ReservationRepository;
import com.cinema.cinema_backend.repository.ScreeningRepository;
import com.cinema.cinema_backend.repository.SeatRepository;
import com.cinema.cinema_backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    public static final int MAX_SEATS = 8;

    private static final Comparator<TicketResponse> BY_SEAT =
            Comparator.comparing(TicketResponse::seatRow).thenComparingInt(TicketResponse::seatCol);

    private final MemberService memberService;
    private final ScheduleService scheduleService;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    /**
     * 예매 + 모의 결제.
     * 1) 회차 행을 FOR UPDATE로 잠가 같은 회차 예매를 순서대로 처리하고
     * 2) 이미 ACTIVE 티켓이 있는 좌석이면 409와 겹친 seat_ids를 돌려준다.
     * 3) 그래도 겹치면 tickets UNIQUE (screening_id, active_seat_id)가 INSERT를 막는다.
     * 좌석 하나라도 실패하면 예약 전체가 롤백된다.
     */
    @Transactional
    public ReservationResponse reserve(Long memberId, ReservationRequest request) {
        Set<Long> seatIds = new LinkedHashSet<>(request.seatIds());
        if (seatIds.size() != request.seatIds().size()) {
            throw CinemaException.badRequest(ErrorCode.INVALID_SEAT, "같은 좌석이 중복으로 선택되었습니다.");
        }
        if (seatIds.isEmpty() || seatIds.size() > MAX_SEATS) {
            throw CinemaException.badRequest(ErrorCode.VALIDATION_FAILED,
                    "좌석은 1석 이상 " + MAX_SEATS + "석 이하로 선택해 주세요.");
        }

        // 트랜잭션의 첫 쿼리로 잠가야 이후 조회가 잠금 이후의 최신 데이터를 본다 (REPEATABLE READ 스냅샷).
        Screening screening = screeningRepository.findByIdForUpdate(request.scheduleId())
                .orElseThrow(() -> CinemaException.notFound("상영회차를 찾을 수 없습니다."));
        Member member = memberService.getMember(memberId);

        LocalDateTime now = LocalDateTime.now(clock);
        if (screening.hasStarted(now)) {
            throw CinemaException.badRequest(ErrorCode.SCHEDULE_STARTED, "이미 상영이 시작된 회차입니다.");
        }
        checkAge(member, screening);

        List<Seat> seats = seatRepository.findAllById(seatIds);
        Long screenId = screening.getScreen().getScreenId();
        if (seats.size() != seatIds.size() || seats.stream().anyMatch(s -> !s.getScreenId().equals(screenId))) {
            throw CinemaException.badRequest(ErrorCode.INVALID_SEAT, "이 상영관에 없는 좌석이 포함되어 있습니다.");
        }

        List<Long> conflicts = ticketRepository.findActiveSeatIdsIn(screening.getScreeningId(), seatIds);
        if (!conflicts.isEmpty()) {
            throw new SeatAlreadyReservedException(conflicts);
        }

        int totalPrice = Math.multiplyExact(screening.getPrice(), seats.size());
        Reservation reservation = reservationRepository.saveAndFlush(
                new Reservation(member, screening, totalPrice, now));

        List<Ticket> tickets = seats.stream()
                .map(seat -> new Ticket(reservation, seat, screening))
                .toList();
        try {
            ticketRepository.saveAllAndFlush(tickets);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_tickets_active_screening_seat")) {
                // 어떤 좌석이 겹쳤는지는 롤백 중인 트랜잭션에서 다시 조회할 수 없어 빈 목록으로 보낸다.
                throw new SeatAlreadyReservedException(List.of());
            }
            throw e;
        }

        return toResponse(reservation, tickets);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long memberId) {
        List<Reservation> reservations = reservationRepository.findByMemberWithScreening(memberId);
        if (reservations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Ticket>> ticketsByReservation = ticketRepository
                .findWithSeatByReservationIds(reservations.stream().map(Reservation::getReservationId).toList())
                .stream()
                .collect(Collectors.groupingBy(t -> t.getId().getReservationId()));

        return reservations.stream()
                .map(r -> toResponse(r, ticketsByReservation.getOrDefault(r.getReservationId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long memberId, Long reservationId) {
        Reservation reservation = reservationRepository.findWithScreening(reservationId)
                .orElseThrow(() -> CinemaException.notFound("예매 내역을 찾을 수 없습니다."));
        checkOwner(reservation, memberId);
        return toResponse(reservation, ticketRepository.findWithSeatByReservationIds(List.of(reservationId)));
    }

    /** 본인 예약만, 상영 시작 전까지 취소할 수 있다. 티켓이 CANCELLED가 되면 같은 좌석을 다시 예매할 수 있다. */
    @Transactional
    public ReservationResponse cancel(Long memberId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> CinemaException.notFound("예매 내역을 찾을 수 없습니다."));
        checkOwner(reservation, memberId);

        if (reservation.isCancelled()) {
            throw new CinemaException(HttpStatus.CONFLICT, ErrorCode.ALREADY_CANCELLED, "이미 취소된 예매입니다.");
        }
        if (reservation.getScreening().hasStarted(LocalDateTime.now(clock))) {
            throw CinemaException.badRequest(ErrorCode.SCHEDULE_STARTED, "이미 상영이 시작된 회차는 취소할 수 없습니다.");
        }

        reservation.cancel();
        reservationRepository.flush();

        return toResponse(reservation, reservation.getTickets());
    }

    private void checkOwner(Reservation reservation, Long memberId) {
        if (!reservation.isOwnedBy(memberId)) {
            throw new CinemaException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "본인의 예매 내역만 조회하거나 취소할 수 있습니다.");
        }
    }

    /** 상영일 기준 만 나이로 관람등급을 검사한다. (프론트 src/lib/age.js와 같은 규칙) */
    private void checkAge(Member member, Screening screening) {
        int minAge = screening.getMovie().getAgeRating().getMinAge();
        if (minAge == 0) {
            return;
        }
        LocalDate screeningDate = screening.getStartsAt().toLocalDate();
        int age = Period.between(member.getBirthDate(), screeningDate).getYears();
        if (age < minAge) {
            throw new CinemaException(HttpStatus.FORBIDDEN, ErrorCode.AGE_RESTRICTED,
                    "만 " + minAge + "세 이상 관람할 수 있는 영화입니다. 상영일 기준 회원님은 만 " + age + "세라 예매할 수 없습니다.");
        }
    }

    private ReservationResponse toResponse(Reservation reservation, List<Ticket> tickets) {
        List<TicketResponse> ticketResponses = tickets.stream()
                .map(TicketResponse::from)
                .sorted(BY_SEAT)
                .toList();
        return ReservationResponse.of(reservation, scheduleService.toResponse(reservation.getScreening()), ticketResponses);
    }
}
