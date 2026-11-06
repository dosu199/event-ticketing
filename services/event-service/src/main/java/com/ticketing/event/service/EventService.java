package com.ticketing.event.service;

import com.ticketing.event.domain.Event;
import com.ticketing.event.domain.EventStatus;
import com.ticketing.event.domain.Seat;
import com.ticketing.event.domain.Venue;
import com.ticketing.event.repository.EventRepository;
import com.ticketing.event.repository.SeatRepository;
import com.ticketing.event.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Use cases for the event catalog. Read methods are read-only transactions by default
 * (class-level annotation); methods that change data override it with @Transactional.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    // Soonest first; id breaks ties so the order is stable across pages.
    private static final Sort LIST_ORDER = Sort.by("startTime").ascending().and(Sort.by("id").ascending());

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final Clock clock;

    // Clock is injected (see ClockConfig) instead of calling Instant.now() directly,
    // so tests can use a fixed time and "is it in the past?" rules stay deterministic.
    public EventService(EventRepository eventRepository, VenueRepository venueRepository,
                        SeatRepository seatRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.seatRepository = seatRepository;
        this.clock = clock;
    }

    public Page<Event> findPage(EventStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, LIST_ORDER);
        return status == null
                ? eventRepository.findAll(pageRequest)
                : eventRepository.findByStatus(status, pageRequest);
    }

    public Event findById(Long id) {
        return eventRepository.findWithVenueById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    public List<Seat> findSeats(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        return seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(eventId);
    }

    @Transactional
    public Event create(Long venueId, String title, String description, Instant startTime) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
        return eventRepository.save(new Event(venue, title.strip(), blankToNull(description), startTime));
    }

    // No explicit save(): the entity is "managed" inside the transaction, so Hibernate
    // detects the changed status at commit and issues the UPDATE (dirty checking).
    @Transactional
    public Event publish(Long id) {
        Event event = findById(id);
        event.publish(Instant.now(clock));
        return event;
    }

    @Transactional
    public Event cancel(Long id) {
        Event event = findById(id);
        event.cancel();
        return event;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
