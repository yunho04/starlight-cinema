
package com.cinema.cinema_backend.service;

import com.cinema.cinema_backend.auth.JwtTokenProvider;
import com.cinema.cinema_backend.dto.LoginRequest;
import com.cinema.cinema_backend.dto.LoginResponse;
import com.cinema.cinema_backend.dto.MemberResponse;
import com.cinema.cinema_backend.dto.SignupRequest;
import com.cinema.cinema_backend.entity.Member;
import com.cinema.cinema_backend.exception.CinemaException;
import com.cinema.cinema_backend.exception.ErrorCode;
import com.cinema.cinema_backend.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 맞지 않습니다.";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId().trim())
                .orElseThrow(() -> new CinemaException(
                        HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, LOGIN_FAILED_MESSAGE));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new CinemaException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, LOGIN_FAILED_MESSAGE);
        }

        String token = jwtTokenProvider.createToken(member.getMemberId());
        return new LoginResponse(token, MemberResponse.from(member));
    }

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        String loginId = request.loginId().trim();
        String phone = normalizePhone(request.phone());
        LocalDate birthDate = request.birthDate();

        if (memberRepository.existsByLoginId(loginId)) {
            throw new CinemaException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_LOGIN_ID, "이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByPhoneNumber(phone)) {
            throw new CinemaException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_PHONE, "이미 가입된 전화번호입니다.");
        }

        Member member = new Member(
                loginId,
                request.name().trim(),
                birthDate,
                passwordEncoder.encode(request.password()),
                phone,
                LocalDateTime.now(clock)
        );

        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 아이디/전화번호로 가입한 경우 UNIQUE 제약조건이 막는다.
            String cause = String.valueOf(e.getMostSpecificCause().getMessage());
            if (cause.contains("uq_members_phone_number")) {
                throw new CinemaException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_PHONE, "이미 가입된 전화번호입니다.");
            }
            throw new CinemaException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_LOGIN_ID, "이미 사용 중인 아이디입니다.");
        }
        return MemberResponse.from(member);
    }

    /** 01012345678, 010-1234-5678 등을 seed 데이터와 같은 010-1234-5678 형식으로 맞춘다. */
    static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        int middle = digits.length() - 4;
        return digits.substring(0, 3) + "-" + digits.substring(3, middle) + "-" + digits.substring(middle);
    }
}
