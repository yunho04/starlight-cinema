package com.cinema.cinema_backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // seed.sql 에 들어간 테스트 계정 해시 ($2y$12$...)
    private static final String ADULT01_HASH = "$2y$12$czKPjaphrwyu35yX96Rjt.pqP7MgbknIsbY8spBhfUp46hQe8paUi";
    private static final String TEEN01_HASH = "$2y$12$cQkvJKsLhoXKqWi41ECpF.XjDhG8QYuvy.65esEUxqAQbNjicgoKW";

    @Test
    void seed_계정의_2y_해시를_검증할_수_있다() {
        assertThat(encoder.matches("test1234", ADULT01_HASH)).isTrue();
        assertThat(encoder.matches("test1234", TEEN01_HASH)).isTrue();
        assertThat(encoder.matches("wrong-password", ADULT01_HASH)).isFalse();
    }

    @Test
    void 새로_만든_해시는_평문과_다르고_검증된다() {
        String hash = encoder.encode("secret1234");
        assertThat(hash).startsWith("$2a$").isNotEqualTo("secret1234");
        assertThat(encoder.matches("secret1234", hash)).isTrue();
    }
}
