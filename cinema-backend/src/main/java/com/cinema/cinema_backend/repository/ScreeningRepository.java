package com.cinema.cinema_backend.repository;

import com.cinema.cinema_backend.entity.Screening;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    @Query("""
            select s from Screening s
            join fetch s.movie
            join fetch s.screen
            where s.movie.movieId = :movieId and s.startsAt >= :from
            order by s.startsAt, s.screen.screenId
            """)
    List<Screening> findByMovieFrom(@Param("movieId") Long movieId, @Param("from") LocalDateTime from);

    @Query("""
            select s from Screening s
            join fetch s.movie
            join fetch s.screen
            where s.screeningId = :screeningId
            """)
    Optional<Screening> findWithMovieAndScreen(@Param("screeningId") Long screeningId);

    /**
     * 예매 시 회차 행을 SELECT ... FOR UPDATE로 잠가 같은 회차 예매 요청을 한 줄로 세운다.
     * 최종 중복 방지는 tickets의 UNIQUE (screening_id, active_seat_id)가 담당한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Screening s where s.screeningId = :screeningId")
    Optional<Screening> findByIdForUpdate(@Param("screeningId") Long screeningId);
}
