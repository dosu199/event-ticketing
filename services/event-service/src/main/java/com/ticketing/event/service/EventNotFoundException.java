package com.ticketing.event.service;

/**
 * The requested event doesn't exist. Mapped to 404 by GlobalExceptionHandler.
 */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long id) {
        super("Event " + id + " not found");
    }
}
