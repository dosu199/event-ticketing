package com.ticketing.event.repository;

import com.ticketing.event.domain.Event;
import com.ticketing.event.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Every page loads its venues in the same query (join), so no N+1.
    // Spring Data runs a second COUNT query to fill totalElements.
    @Override
    @EntityGraph(attributePaths = "venue")
    Page<Event> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    Optional<Event> findWithVenueById(Long id);
}
