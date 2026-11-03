package com.ticketing.event.api;

import com.ticketing.event.domain.Event;
import com.ticketing.event.domain.EventStatus;

import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        Instant startTime,
        EventStatus status,
        VenueSummary venue
) {

    public record VenueSummary(Long id, String name, String city) {
    }

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartTime(),
                event.getStatus(),
                new VenueSummary(
                        event.getVenue().getId(),
                        event.getVenue().getName(),
                        event.getVenue().getCity()
                )
        );
    }
}