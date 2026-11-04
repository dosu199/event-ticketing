package com.ticketing.event.api;

import com.ticketing.event.domain.EventStatus;
import com.ticketing.event.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    // status is optional; an unknown value (e.g. ?status=FOO) is rejected with 400 by Spring.
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
}
