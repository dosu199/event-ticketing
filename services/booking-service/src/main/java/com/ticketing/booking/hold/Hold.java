package com.ticketing.booking.hold;

import java.time.Instant;
import java.util.List;

/** Seats reserved for one user for a limited time. */
public record Hold(String id, String userId, long eventId, List<Long> seatIds, Instant expiresAt) {
}
