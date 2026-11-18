package com.cinema.cinema_backend.dto;

import com.cinema.cinema_backend.entity.Movie;
import com.cinema.cinema_backend.entity.Screening;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** 상영회차(screenings) + 영화 + 상영관 정보. 프론트에서는 schedule이라고 부른다. */
public record ScheduleResponse(

        // DB: screening_id
        @JsonProperty("schedule_id")
        Long scheduleId,

        @JsonProperty("movie_id")
        Long movieId,

        @JsonProperty("movie_title")
        String movieTitle,

        @JsonProperty("age_rating")
        String ageRating,

        @JsonProperty("poster_url")
        String posterUrl,

        @JsonProperty("screen_id")
        Long screenId,

        @JsonProperty("screen_name")
        String screenName,

        // DB: starts_at
        @JsonProperty("start_time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startTime,

        // DB: ends_at
        @JsonProperty("end_time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endTime,

        int price

) {
    public static ScheduleResponse from(Screening screening, String posterUrl) {
        Movie movie = screening.getMovie();
        return new ScheduleResponse(
                screening.getScreeningId(),
                movie.getMovieId(),
                movie.getTitle(),
                movie.getAgeRating().getApiValue(),
                posterUrl,
                screening.getScreen().getScreenId(),
                screening.getScreen().getScreenName(),
                screening.getStartsAt(),
                screening.getEndsAt(),
                screening.getPrice()
        );
    }
}
