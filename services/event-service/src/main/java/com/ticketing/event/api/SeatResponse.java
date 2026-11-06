package com.ticketing.event.api;

import com.ticketing.event.domain.Seat;
import com.ticketing.event.domain.SeatCategory;

import java.math.BigDecimal;

public record SeatResponse(
        Long id,
        String rowLabel,
        Integer seatNumber,
        SeatCategory category,
        BigDecimal price
) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getCategory(), seat.getPrice());
    }
}
