package com.cinema.cinema_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Convert(converter = AgeRatingConverter.class)
    @Column(name = "age_rating", nullable = false, columnDefinition = "enum('ALL','12','15','18')")
    private AgeRating ageRating;

    @Column(name = "runtime_minutes", nullable = false, columnDefinition = "smallint unsigned")
    private Integer runtimeMinutes;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
