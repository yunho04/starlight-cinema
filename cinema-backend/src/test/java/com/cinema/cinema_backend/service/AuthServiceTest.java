package com.cinema.cinema_backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    @Test
    void 전화번호를_하이픈_형식으로_맞춘다() {
        assertThat(AuthService.normalizePhone("01012345678")).isEqualTo("010-1234-5678");
        assertThat(AuthService.normalizePhone("010-1234-5678")).isEqualTo("010-1234-5678");
        assertThat(AuthService.normalizePhone("0111234567")).isEqualTo("011-123-4567");
    }
}
