
package com.cinema.cinema_backend.controller;

import com.cinema.cinema_backend.dto.MovieResponse;
import com.cinema.cinema_backend.dto.ScheduleResponse;
import com.cinema.cinema_backend.service.MovieService;

import com.cinema.cinema_backend.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;
    private final ScheduleService scheduleService;


    @GetMapping
    public List<MovieResponse> getMovies() {
        return movieService.getMovies();
    }

    // 특정 영화 상세 정보
    @GetMapping("/{id}")
    public MovieResponse getMovie(
            @PathVariable Long id
    ) {
        return movieService.getMovie(id);
    }

    // 영화별 상영회차 (오늘 이후, 시작 시각 순)
    @GetMapping("/{id}/schedules")
    public List<ScheduleResponse> getSchedules(
            @PathVariable Long id
    ) {
        return scheduleService.getSchedulesByMovie(id);
    }
}
