package com.ticketing.booking.api;

import com.ticketing.booking.hold.Availability;

import java.util.List;

/**
 * @param unavailableSeatIds seats the caller can't take right now (held by others), sorted
 * @param myHold             the caller's active hold for this event, or null
 */
public record AvailabilityResponse(long eventId, List<Long> unavailableSeatIds, HoldResponse myHold) {

    public static AvailabilityResponse from(Availability availability) {
        return new AvailabilityResponse(
                availability.eventId(),
                availability.unavailableSeatIds().stream().sorted().toList(),
                HoldResponse.from(availability.myHold()));
    }
}
