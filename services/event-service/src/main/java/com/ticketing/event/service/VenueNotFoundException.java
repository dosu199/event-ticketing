package com.ticketing.event.service;

/**
 * A request referenced a venue id that doesn't exist. Mapped to 400 by GlobalExceptionHandler:
 * the URL is fine, the request body is wrong.
 */
public class VenueNotFoundException extends RuntimeException {

    public VenueNotFoundException(Long id) {
        super("Venue " + id + " does not exist");
    }
}
