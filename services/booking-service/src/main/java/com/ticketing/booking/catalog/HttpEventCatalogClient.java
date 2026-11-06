package com.ticketing.booking.catalog;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Synchronous HTTP calls to event-service with RestClient.
 * Translates HTTP failures into exceptions that mean something to booking-service.
 */
@Component
public class HttpEventCatalogClient implements EventCatalogClient {

    private static final ParameterizedTypeReference<List<CatalogSeat>> SEAT_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpEventCatalogClient(RestClient eventServiceRestClient) {
        this.restClient = eventServiceRestClient;
    }

    @Override
    public CatalogEvent getEvent(long eventId) {
        try {
            return restClient.get().uri("/events/{id}", eventId).retrieve().body(CatalogEvent.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new EventNotFoundException(eventId);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("event-service is unavailable", e);
        }
    }

    @Override
    public List<CatalogSeat> getSeats(long eventId) {
        try {
            List<CatalogSeat> seats = restClient.get().uri("/events/{id}/seats", eventId).retrieve().body(SEAT_LIST);
            return seats == null ? List.of() : seats;
        } catch (HttpClientErrorException.NotFound e) {
            throw new EventNotFoundException(eventId);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("event-service is unavailable", e);
        }
    }
}
