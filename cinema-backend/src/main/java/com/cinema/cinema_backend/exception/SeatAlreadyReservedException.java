package com.cinema.cinema_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/** 409: 이미 예매된 좌석이 포함된 경우. seat_ids로 겹친 좌석을 알려준다. */
@Getter
public class SeatAlreadyReservedException extends CinemaException {

    private final List<Long> seatIds;

    public SeatAlreadyReservedException(List<Long> seatIds) {
        super(HttpStatus.CONFLICT, ErrorCode.SEAT_ALREADY_RESERVED, "이미 예매된 좌석이 포함되어 있습니다.");
        this.seatIds = List.copyOf(seatIds);
    }
}
