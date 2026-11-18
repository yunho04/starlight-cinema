package com.cinema.cinema_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    // DB의 DATETIME 값은 한국 시간으로 저장한다는 스키마 규칙에 맞춘다.
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // seed.sql의 $2y$12$ 해시도 검증 가능
        return new BCryptPasswordEncoder();
    }
}
