package com.cinema.cinema_backend.service;

import com.cinema.cinema_backend.dto.MemberResponse;
import com.cinema.cinema_backend.entity.Member;
import com.cinema.cinema_backend.exception.CinemaException;
import com.cinema.cinema_backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponse getMe(Long memberId) {
        return MemberResponse.from(getMember(memberId));
    }

    /** 토큰은 유효하지만 회원이 없는 경우도 다시 로그인하도록 401로 처리한다. */
    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> CinemaException.unauthorized("회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."));
    }
}
