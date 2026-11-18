package com.cinema.cinema_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** tickets 테이블의 복합 기본키 (reservation_id, seat_id) */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TicketId implements Serializable {

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "seat_id")
    private Long seatId;
}
