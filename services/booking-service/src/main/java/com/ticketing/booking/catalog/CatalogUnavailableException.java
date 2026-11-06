package com.ticketing.booking.catalog;

/** event-service didn't answer (down, timeout) or answered with an unexpected error. */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
