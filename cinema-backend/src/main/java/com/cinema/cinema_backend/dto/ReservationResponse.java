package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Reservation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/** 예매 1건. 예매 완료 화면과 마이페이지 목록에서 같은 모양을 쓴다. */
public record ReservationResponse(

        @JsonProperty("reservation_id")
        Long reservationId,

        // CONFIRMED / CANCELLED
        String status,

        // DB: booked_at
        @JsonProperty("reserved_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime reservedAt,

        @JsonProperty("total_price")
        int totalPrice,

        ScheduleResponse schedule,

        List<TicketResponse> tickets

) {
    public static ReservationResponse of(Reservation reservation, ScheduleResponse schedule,
                                         List<TicketResponse> tickets) {
        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getStatus().name(),
                reservation.getBookedAt(),
                reservation.getTotalPrice(),
                schedule,
                tickets
        );
    }
}
