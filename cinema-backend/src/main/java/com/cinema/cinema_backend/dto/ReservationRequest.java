package com.cinema.cinema_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReservationRequest(

        // DB: screening_id
        @JsonProperty("schedule_id")
        @NotNull(message = "상영회차를 선택해 주세요.")
        @Positive(message = "상영회차 번호가 올바르지 않습니다.")
        Long scheduleId,

        @JsonProperty("seat_ids")
        @NotEmpty(message = "좌석을 1개 이상 선택해 주세요.")
        @Size(max = 8, message = "한 번에 최대 8석까지 예매할 수 있습니다.")
        List<@NotNull(message = "좌석 번호가 올바르지 않습니다.") Long> seatIds

) {}
