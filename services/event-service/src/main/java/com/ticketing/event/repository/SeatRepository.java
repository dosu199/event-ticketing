package com.ticketing.event.repository;

import com.ticketing.event.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // "EventId" resolves to seat.event.id; uses the uq_seat_event_row_number index.
    List<Seat> findByEventIdOrderByRowLabelAscSeatNumberAsc(Long eventId);
}
