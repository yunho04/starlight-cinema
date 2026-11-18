package com.cinema.cinema_backend.controller;

import com.cinema.cinema_backend.dto.SeatMapResponse;
import com.cinema.cinema_backend.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 회차 정보 + 상영관 좌석 목록 (reserved: 예매된 좌석 여부)
    @GetMapping("/{id}/seats")
    public SeatMapResponse getSeats(@PathVariable Long id) {
        return scheduleService.getSeatMap(id);
    }
}
