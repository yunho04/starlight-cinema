package com.cinema.cinema_backend.repository;

import com.cinema.cinema_backend.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select r from Reservation r
            join fetch r.screening s
            join fetch s.movie
            join fetch s.screen
            where r.member.memberId = :memberId
            order by r.bookedAt desc, r.reservationId desc
            """)
    List<Reservation> findByMemberWithScreening(@Param("memberId") Long memberId);

    @Query("""
            select r from Reservation r
            join fetch r.screening s
            join fetch s.movie
            join fetch s.screen
            where r.reservationId = :reservationId
            """)
    Optional<Reservation> findWithScreening(@Param("reservationId") Long reservationId);

    /** 취소 시 같은 예약을 동시에 두 번 취소하지 않도록 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.reservationId = :reservationId")
    Optional<Reservation> findByIdForUpdate(@Param("reservationId") Long reservationId);
}
