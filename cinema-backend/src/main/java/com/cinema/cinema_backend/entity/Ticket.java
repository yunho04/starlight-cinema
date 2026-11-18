package com.cinema.cinema_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * 예약 좌석(티켓).
 * active_seat_id는 DB가 계산하는 생성 열(STORED)이므로 매핑하지 않는다.
 * UNIQUE (screening_id, active_seat_id)가 같은 회차·같은 좌석의 ACTIVE 티켓 중복을 막는다.
 */
@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket implements Persistable<TicketId> {

    @EmbeddedId
    private TicketId id;

    @MapsId("reservationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @MapsId("seatId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    // (reservation_id, screening_id), (screening_id, screen_id) 복합 FK에 쓰이는 값
    @Column(name = "screening_id", nullable = false)
    private Long screeningId;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "paid_price", nullable = false, columnDefinition = "int unsigned")
    private Integer paidPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", nullable = false, columnDefinition = "enum('ACTIVE','CANCELLED')")
    private TicketStatus status;

    public Ticket(Reservation reservation, Seat seat, Screening screening) {
        this.id = new TicketId(reservation.getReservationId(), seat.getSeatId());
        this.reservation = reservation;
        this.seat = seat;
        this.screeningId = screening.getScreeningId();
        this.screenId = screening.getScreen().getScreenId();
        this.paidPrice = screening.getPrice();
        this.status = TicketStatus.ACTIVE;
        reservation.getTickets().add(this);
    }

    // 복합키를 직접 채우므로 save() 시 merge(SELECT 후 INSERT) 대신 persist 하도록 새 엔티티 여부를 알려준다.
    @Transient
    private boolean isNew = true;

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
