
package com.cinema.cinema_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DbCheckController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/db-check")
    public Map<String, Object> checkDatabase() {

        Integer movieCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM movies",
                        Integer.class
                );

        Integer memberCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM members",
                        Integer.class
                );

        return Map.of(
                "connected", true,
                "movies", movieCount,
                "members", memberCount
        );
    }
}

