package com.ticketing.booking.hold;

/** The request breaks a rule that needs data to check (seat not in event, too many seats, event not on sale). */
public class InvalidHoldRequestException extends RuntimeException {

    public InvalidHoldRequestException(String message) {
        super(message);
    }
}
