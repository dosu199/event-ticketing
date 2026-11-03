package com.ticketing.event.repository;

import com.ticketing.event.domain.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = "venue")
    List<Event> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = "venue")
    Optional<Event> findWithVenueById(Long id);
}