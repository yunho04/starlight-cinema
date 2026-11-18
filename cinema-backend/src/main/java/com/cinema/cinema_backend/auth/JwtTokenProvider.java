package com.cinema.cinema_backend.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** 회원 번호(member_id)를 subject로 담는 HS256 JWT를 발급·검증한다. */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes,
            Clock clock
    ) {
        this.key = createKey(secret);
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    private static SecretKey createKey(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET이 설정되지 않아 임시 서명 키를 생성합니다. 서버를 재시작하면 기존 토큰은 무효가 됩니다.");
            return Jwts.SIG.HS256.key().build();
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long memberId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** 유효한 토큰이면 member_id, 위조·만료·형식 오류면 empty */
    public Optional<Long> parseMemberId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.of(Long.valueOf(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
