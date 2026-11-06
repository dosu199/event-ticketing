package com.ticketing.booking.hold;

/** The user already has an active hold for this event (one hold per user per event). */
public class AlreadyHoldingException extends RuntimeException {

    public AlreadyHoldingException(long eventId) {
        super("You already hold seats for event " + eventId + ". Release them first.");
    }
}
