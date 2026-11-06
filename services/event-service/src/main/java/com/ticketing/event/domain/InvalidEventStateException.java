package com.ticketing.event.domain;

/**
 * A state change that the event's rules don't allow, e.g. publishing a cancelled event.
 * Mapped to 409 Conflict by GlobalExceptionHandler.
 */
public class InvalidEventStateException extends RuntimeException {

    public InvalidEventStateException(String message) {
        super(message);
    }
}
