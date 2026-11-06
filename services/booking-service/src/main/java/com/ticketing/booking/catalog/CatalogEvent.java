package com.ticketing.booking.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The parts of event-service's EventResponse that booking-service needs.
 * Each service keeps its own small copy of another service's data shape and ignores
 * the rest, so event-service can add fields without breaking booking-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogEvent(Long id, String title, String status) {

    public boolean isOnSale() {
        return "PUBLISHED".equals(status);
    }
}
