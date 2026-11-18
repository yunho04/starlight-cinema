
package com.cinema.cinema_backend.service;

import com.cinema.cinema_backend.dto.MovieResponse;
import com.cinema.cinema_backend.entity.Movie;
import com.cinema.cinema_backend.exception.CinemaException;
import com.cinema.cinema_backend.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final PosterUrlResolver posterUrlResolver;

    // 전체 영화 조회
    public List<MovieResponse> getMovies() {
        return movieRepository.findAll(Sort.by("movieId")).stream()
                .map(this::toResponse)
                .toList();
    }

    // 특정 영화 상세 조회
    public MovieResponse getMovie(Long id) {
        return toResponse(findMovie(id));
    }

    public Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> CinemaException.notFound("영화를 찾을 수 없습니다."));
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.from(movie, posterUrlResolver.resolve(movie.getMovieId()));
    }
}
