package com.cinema.cinema_backend.auth;

import com.cinema.cinema_backend.exception.CinemaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/** Authorization: Bearer <token> 헤더를 검증하고 member_id를 요청 속성에 저장한다. */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String MEMBER_ID_ATTRIBUTE = "loginMemberId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw CinemaException.unauthorized("로그인이 필요합니다.");
        }
        Long memberId = jwtTokenProvider.parseMemberId(header.substring(BEARER_PREFIX.length()).trim())
                .orElseThrow(() -> CinemaException.unauthorized("로그인이 만료되었습니다. 다시 로그인해 주세요."));
        request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
        return true;
    }
}
