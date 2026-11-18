package com.cinema.cinema_backend.service;

import com.cinema.cinema_backend.dto.ScheduleResponse;
import com.cinema.cinema_backend.dto.SeatMapResponse;
import com.cinema.cinema_backend.dto.SeatResponse;
import com.cinema.cinema_backend.entity.Screening;
import com.cinema.cinema_backend.exception.CinemaException;
import com.cinema.cinema_backend.repository.ScreeningRepository;
import com.cinema.cinema_backend.repository.SeatRepository;
import com.cinema.cinema_backend.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 상영회차(screenings) 조회와 좌석 현황 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final MovieService movieService;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final PosterUrlResolver posterUrlResolver;
    private final Clock clock;

    /** 오늘(한국 시간 0시) 이후 회차. 이미 시작한 오늘 회차도 내려주고 프론트가 '상영 시작'으로 막는다. */
    public List<ScheduleResponse> getSchedulesByMovie(Long movieId) {
        movieService.findMovie(movieId);
        return screeningRepository.findByMovieFrom(movieId, LocalDate.now(clock).atStartOfDay()).stream()
                .map(this::toResponse)
                .toList();
    }

    public SeatMapResponse getSeatMap(Long scheduleId) {
        Screening screening = findScreening(scheduleId);
        Set<Long> reservedSeatIds = new HashSet<>(ticketRepository.findActiveSeatIds(scheduleId));

        List<SeatResponse> seats = seatRepository
                .findByScreen_ScreenIdOrderBySeatRowAscSeatNoAsc(screening.getScreen().getScreenId())
                .stream()
                .map(seat -> SeatResponse.from(seat, reservedSeatIds.contains(seat.getSeatId())))
                .toList();

        return new SeatMapResponse(toResponse(screening), seats);
    }

    public Screening findScreening(Long scheduleId) {
        return screeningRepository.findWithMovieAndScreen(scheduleId)
                .orElseThrow(() -> CinemaException.notFound("상영회차를 찾을 수 없습니다."));
    }

    public ScheduleResponse toResponse(Screening screening) {
        return ScheduleResponse.from(screening, posterUrlResolver.resolve(screening.getMovie().getMovieId()));
    }
}
