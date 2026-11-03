package com.ticketing.event.service;

import com.ticketing.event.domain.Event;
import com.ticketing.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAllByOrderByStartTimeAsc();
    }

    public Event findById(Long id) {
        return eventRepository.findWithVenueById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}