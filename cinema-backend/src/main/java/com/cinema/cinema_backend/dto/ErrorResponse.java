package com.cinema.cinema_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        String code,

        String message,

        // 409 SEAT_ALREADY_RESERVED 에서만 채워짐
        @JsonProperty("seat_ids")
        List<Long> seatIds

) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
}
