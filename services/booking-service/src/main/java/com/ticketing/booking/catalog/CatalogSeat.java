package com.ticketing.booking.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSeat(Long id, String rowLabel, Integer seatNumber, String category, BigDecimal price) {
}
