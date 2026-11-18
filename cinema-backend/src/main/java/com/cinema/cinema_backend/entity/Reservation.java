package com.cinema.cinema_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "total_price", nullable = false, columnDefinition = "int unsigned")
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false,
            columnDefinition = "enum('CONFIRMED','CANCELLED')")
    private ReservationStatus status;

    @OneToMany(mappedBy = "reservation")
    private List<Ticket> tickets = new ArrayList<>();

    public Reservation(Member member, Screening screening, int totalPrice, LocalDateTime bookedAt) {
        this.member = member;
        this.screening = screening;
        this.totalPrice = totalPrice;
        this.bookedAt = bookedAt;
        this.status = ReservationStatus.CONFIRMED;
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getMemberId().equals(memberId);
    }

    public boolean isCancelled() {
        return status == ReservationStatus.CANCELLED;
    }

    /** 예약과 소속 티켓을 모두 취소 상태로 바꾼다. 티켓의 active_seat_id가 NULL이 되어 좌석 재예매가 가능해진다. */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        tickets.forEach(Ticket::cancel);
    }
}
