package com.ticketing.booking.catalog;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(long eventId) {
        super("Event " + eventId + " not found");
    }
}
