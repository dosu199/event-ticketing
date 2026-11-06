package com.ticketing.booking.hold;

import java.util.List;

/** One or more seats are already held by someone else. Nothing was held (all-or-nothing). */
public class SeatsUnavailableException extends RuntimeException {

    private final List<Long> seatIds;

    public SeatsUnavailableException(List<Long> seatIds) {
        super("Seats already taken: " + seatIds);
        this.seatIds = List.copyOf(seatIds);
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }
}
