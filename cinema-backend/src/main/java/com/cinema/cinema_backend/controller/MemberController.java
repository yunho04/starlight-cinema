package com.cinema.cinema_backend.controller;

import com.cinema.cinema_backend.auth.LoginMember;
import com.cinema.cinema_backend.dto.MemberResponse;
import com.cinema.cinema_backend.dto.ReservationResponse;
import com.cinema.cinema_backend.service.MemberService;
import com.cinema.cinema_backend.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final ReservationService reservationService;

    // 로그인한 회원 정보
    @GetMapping("/me")
    public MemberResponse getMe(@LoginMember Long memberId) {
        return memberService.getMe(memberId);
    }

    // 내 예매 내역 (최신순, 취소된 예매 포함 - status로 구분)
    @GetMapping("/me/reservations")
    public List<ReservationResponse> getMyReservations(@LoginMember Long memberId) {
        return reservationService.getMyReservations(memberId);
    }
}
