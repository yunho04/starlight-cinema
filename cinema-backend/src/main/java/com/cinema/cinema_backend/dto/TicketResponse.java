package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Ticket;
import com.fasterxml.jackson.annotation.JsonProperty;

/** tickets는 (reservation_id, seat_id) 복합키라 별도 ticket_id가 없다. */
public record TicketResponse(

        @JsonProperty("seat_id")
        Long seatId,

        @JsonProperty("seat_row")
        String seatRow,

        @JsonProperty("seat_col")
        int seatCol,

        // DB: paid_price
        int price,

        // ACTIVE / CANCELLED
        String status

) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getSeat().getSeatId(),
                ticket.getSeat().getSeatRow(),
                ticket.getSeat().getSeatNo(),
                ticket.getPaidPrice(),
                ticket.getStatus().name()
        );
    }
}
