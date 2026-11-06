package com.ticketing.booking.api;

import com.ticketing.booking.hold.Hold;

import java.time.Instant;
import java.util.List;

public record HoldResponse(String holdId, long eventId, List<Long> seatIds, Instant expiresAt) {

    public static HoldResponse from(Hold hold) {
        return hold == null ? null : new HoldResponse(hold.id(), hold.eventId(), hold.seatIds(), hold.expiresAt());
    }
}
