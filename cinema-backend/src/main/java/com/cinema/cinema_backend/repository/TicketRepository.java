package com.cinema.cinema_backend.repository;

import com.cinema.cinema_backend.entity.Ticket;
import com.cinema.cinema_backend.entity.TicketId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, TicketId> {

    @Query("""
            select t.id.seatId from Ticket t
            where t.screeningId = :screeningId
              and t.status = com.cinema.cinema_backend.entity.TicketStatus.ACTIVE
            """)
    List<Long> findActiveSeatIds(@Param("screeningId") Long screeningId);

    @Query("""
            select t.id.seatId from Ticket t
            where t.screeningId = :screeningId
              and t.status = com.cinema.cinema_backend.entity.TicketStatus.ACTIVE
              and t.id.seatId in :seatIds
            order by t.id.seatId
            """)
    List<Long> findActiveSeatIdsIn(@Param("screeningId") Long screeningId,
                                   @Param("seatIds") Collection<Long> seatIds);

    @Query("""
            select t from Ticket t
            join fetch t.seat
            where t.id.reservationId in :reservationIds
            """)
    List<Ticket> findWithSeatByReservationIds(@Param("reservationIds") Collection<Long> reservationIds);
}
