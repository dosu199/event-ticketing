package com.ticketing.booking.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of POST /holds. The exact per-hold maximum is configurable (booking.hold.max-seats)
 * and enforced by HoldService; @Size here is only a sanity cap.
 */
public record CreateHoldRequest(
        @NotNull Long eventId,
        @NotEmpty @Size(max = 50) List<@NotNull Long> seatIds
) {
}
