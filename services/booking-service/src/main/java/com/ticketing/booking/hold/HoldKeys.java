package com.ticketing.booking.hold;

/**
 * Redis key names, in one place.
 *
 * <pre>
 * hold:event:{eventId}            hash   seatId -> holdId, each field expires on its own
 * hold:{holdId}                   hash   userId, eventId, seatIds, expiresAt
 * hold:user:{eventId}:{userId}    string holdId (one active hold per user per event)
 * </pre>
 */
final class HoldKeys {

    private HoldKeys() {
    }

    static String eventSeats(long eventId) {
        return "hold:event:" + eventId;
    }

    static String hold(String holdId) {
        return "hold:" + holdId;
    }

    static String userHold(long eventId, String userId) {
        return "hold:user:" + eventId + ":" + userId;
    }
}
