
package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Movie;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MovieResponse(

        @JsonProperty("movie_id")
        Long movieId,

        String title,

        // DB: runtime_minutes
        @JsonProperty("running_time")
        int runningTime,

        // DB '18' → API '19'
        @JsonProperty("age_rating")
        String ageRating,

        // DB에 개봉일 컬럼이 없어 null (프론트는 '미정'으로 표시)
        @JsonProperty("release_date")
        String releaseDate,

        @JsonProperty("poster_url")
        String posterUrl,

        String description

) {
    public static MovieResponse from(Movie movie, String posterUrl) {
        return new MovieResponse(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getRuntimeMinutes(),
                movie.getAgeRating().getApiValue(),
                null,
                posterUrl,
                movie.getDescription()
        );
    }
}
