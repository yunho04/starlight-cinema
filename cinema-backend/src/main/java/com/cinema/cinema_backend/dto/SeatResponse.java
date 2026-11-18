package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Seat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SeatResponse(

        @JsonProperty("seat_id")
        Long seatId,

        @JsonProperty("seat_row")
        String seatRow,

        // DB: seat_no
        @JsonProperty("seat_col")
        int seatCol,

        // 해당 회차에 ACTIVE 티켓이 있으면 true
        boolean reserved

) {
    public static SeatResponse from(Seat seat, boolean reserved) {
        return new SeatResponse(seat.getSeatId(), seat.getSeatRow(), seat.getSeatNo(), reserved);
    }
}
