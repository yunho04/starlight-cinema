package com.cinema.cinema_backend.controller;

import com.cinema.cinema_backend.auth.LoginMember;
import com.cinema.cinema_backend.dto.ReservationRequest;
import com.cinema.cinema_backend.dto.ReservationResponse;
import com.cinema.cinema_backend.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    // 예매 + 모의 결제 (실제 PG 연동 없음)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(
            @LoginMember Long memberId,
            @Valid @RequestBody ReservationRequest request
    ) {
        return reservationService.reserve(memberId, request);
    }

    // 예매 1건 조회 (본인 것만)
    @GetMapping("/{id}")
    public ReservationResponse getReservation(
            @LoginMember Long memberId,
            @PathVariable Long id
    ) {
        return reservationService.getReservation(memberId, id);
    }

    // 예매 취소 (본인 것만, 상영 시작 전까지)
    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(
            @LoginMember Long memberId,
            @PathVariable Long id
    ) {
        return reservationService.cancel(memberId, id);
    }
}
