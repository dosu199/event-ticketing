package com.ticketing.booking.hold;

import java.util.Set;

/**
 * Seat availability for one event, from one user's point of view.
 *
 * @param unavailableSeatIds seats held by OTHER users (Phase 4 adds sold seats)
 * @param myHold             the caller's own active hold for this event, or null
 */
public record Availability(long eventId, Set<Long> unavailableSeatIds, Hold myHold) {
}
