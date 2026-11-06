package com.ticketing.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed view of the "booking.*" settings in application.yml.
 * Binding to a record keeps the values immutable and validated at startup.
 */
@ConfigurationProperties(prefix = "booking")
public record BookingProperties(Hold hold) {

    /**
     * @param ttl      how long seats stay reserved (e.g. PT10M = 10 minutes)
     * @param maxSeats maximum seats in a single hold
     */
    public record Hold(Duration ttl, int maxSeats) {
    }
}
