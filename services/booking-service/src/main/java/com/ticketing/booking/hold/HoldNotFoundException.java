package com.ticketing.booking.hold;

/**
 * The hold doesn't exist, has expired, or belongs to another user. All three look the same
 * to the caller on purpose: we don't reveal that someone else's hold exists.
 */
public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(String holdId) {
        super("Hold " + holdId + " not found or expired");
    }
}
