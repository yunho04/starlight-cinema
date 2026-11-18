package com.cinema.cinema_backend.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes!!";
    private static final Instant NOW = Instant.parse("2026-11-16T03:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void 발급한_토큰에서_회원번호를_꺼낸다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 120, clock);
        String token = provider.createToken(42L);
        assertThat(provider.parseMemberId(token)).contains(42L);
    }

    @Test
    void 위조되거나_형식이_틀린_토큰은_거부한다() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 120, clock);
        JwtTokenProvider other = new JwtTokenProvider("another-secret-key-that-is-at-least-32-bytes", 120, clock);
        String token = other.createToken(1L);

        assertThat(provider.parseMemberId(token)).isEmpty();
        assertThat(provider.parseMemberId("not-a-jwt")).isEmpty();
        assertThat(provider.parseMemberId("")).isEmpty();
    }

    @Test
    void 만료된_토큰은_거부한다() {
        String token = new JwtTokenProvider(SECRET, 120, clock).createToken(1L);
        Clock later = Clock.fixed(NOW.plus(Duration.ofMinutes(121)), ZoneOffset.UTC);
        assertThat(new JwtTokenProvider(SECRET, 120, later).parseMemberId(token)).isEmpty();
    }

    @Test
    void 짧은_비밀키는_시작할_때_거부한다() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", 120, clock))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 비밀키가_없으면_임시키로_동작한다() {
        JwtTokenProvider provider = new JwtTokenProvider("", 120, clock);
        assertThat(provider.parseMemberId(provider.createToken(7L))).contains(7L);
    }
}
