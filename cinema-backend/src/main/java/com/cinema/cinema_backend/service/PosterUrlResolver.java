package com.cinema.cinema_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** movies 테이블에 포스터 컬럼이 없어서 movie_id로 포스터 경로를 만든다. */
@Component
public class PosterUrlResolver {

    private final String pattern;

    public PosterUrlResolver(@Value("${app.poster-url-pattern:/posters/%d.svg}") String pattern) {
        this.pattern = pattern;
    }

    public String resolve(Long movieId) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        return String.format(pattern, movieId);
    }
}
