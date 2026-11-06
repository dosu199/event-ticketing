package com.ticketing.event.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body of POST /events. Validation runs before the controller method is called
 * (@Valid); failures become a 400 Problem Detail listing each invalid field.
 * Limits match the database columns, so invalid input never reaches the database.
 */
public record CreateEventRequest(
        @NotNull Long venueId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @NotNull @Future Instant startTime
) {
}
