package com.ticketing.booking.catalog;

import java.util.List;

/**
 * What booking-service needs from event-service. An interface, so tests can replace the
 * HTTP implementation with an in-memory fake.
 */
public interface EventCatalogClient {

    /** @throws EventNotFoundException if the event doesn't exist */
    CatalogEvent getEvent(long eventId);

    /** @throws EventNotFoundException if the event doesn't exist */
    List<CatalogSeat> getSeats(long eventId);
}
