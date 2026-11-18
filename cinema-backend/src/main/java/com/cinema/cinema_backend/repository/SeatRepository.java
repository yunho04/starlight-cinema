package com.cinema.cinema_backend.repository;

import com.cinema.cinema_backend.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreen_ScreenIdOrderBySeatRowAscSeatNoAsc(Long screenId);
}
