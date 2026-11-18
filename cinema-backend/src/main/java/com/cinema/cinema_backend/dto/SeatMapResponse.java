package com.cinema.cinema_backend.dto;

import java.util.List;

public record SeatMapResponse(
        ScheduleResponse schedule,
        List<SeatResponse> seats
) {}
