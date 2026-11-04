package com.ticketing.event.service;

import com.ticketing.event.domain.Event;
import com.ticketing.event.domain.EventStatus;
import com.ticketing.event.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    // Soonest events first. "id" breaks ties: many events share a start time, and
    // without a unique tie-breaker the same event could appear on two pages.
    private static final Sort LIST_ORDER = Sort.by("startTime").ascending().and(Sort.by("id").ascending());

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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
}
