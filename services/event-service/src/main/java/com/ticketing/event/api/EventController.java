package com.ticketing.event.api;

import com.ticketing.event.domain.Event;
import com.ticketing.event.domain.EventStatus;
import com.ticketing.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // GET /events?status=PUBLISHED&page=0&size=20
    @GetMapping
    public PageResponse<EventResponse> getEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageResponse.from(eventService.findPage(status, page, size), EventResponse::from);
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id) {
        return EventResponse.from(eventService.findById(id));
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long id) {
        return eventService.findSeats(id).stream().map(SeatResponse::from).toList();
    }

    // 201 Created + Location header pointing at the new resource (REST convention).
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        Event event = eventService.create(request.venueId(), request.title(), request.description(), request.startTime());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(event.getId())
                .toUri();
        return ResponseEntity.created(location).body(EventResponse.from(event));
    }

    // State changes are modelled as actions (POST .../publish) rather than
    // "PATCH status=PUBLISHED": the server owns the rules, the client states its intent.
    @PostMapping("/{id}/publish")
    public EventResponse publishEvent(@PathVariable Long id) {
        return EventResponse.from(eventService.publish(id));
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancelEvent(@PathVariable Long id) {
        return EventResponse.from(eventService.cancel(id));
    }
}
