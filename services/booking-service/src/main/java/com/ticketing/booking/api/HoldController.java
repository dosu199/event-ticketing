package com.ticketing.booking.api;

import com.ticketing.booking.hold.HoldService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Through the gateway these are under /api/booking (e.g. /api/booking/holds).
 */
@RestController
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    // Polled by the seat picker. Works without a user id (then "myHold" is always null).
    @GetMapping("/events/{eventId}/availability")
    public AvailabilityResponse availability(
            @PathVariable long eventId,
            @RequestHeader(name = UserIds.HEADER, required = false) String userId) {
        return AvailabilityResponse.from(holdService.availability(eventId, UserIds.optional(userId)));
    }

    @PostMapping("/holds")
    public ResponseEntity<HoldResponse> createHold(
            @RequestHeader(UserIds.HEADER) String userId,
            @Valid @RequestBody CreateHoldRequest request) {
        HoldResponse hold = HoldResponse.from(
                holdService.hold(UserIds.require(userId), request.eventId(), request.seatIds()));
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(hold.holdId()).toUri())
                .body(hold);
    }

    @GetMapping("/holds/{holdId}")
    public HoldResponse getHold(@RequestHeader(UserIds.HEADER) String userId, @PathVariable String holdId) {
        return HoldResponse.from(holdService.getHold(UserIds.require(userId), holdId));
    }

    @DeleteMapping("/holds/{holdId}")
    public ResponseEntity<Void> releaseHold(@RequestHeader(UserIds.HEADER) String userId, @PathVariable String holdId) {
        holdService.release(UserIds.require(userId), holdId);
        return ResponseEntity.noContent().build();
    }
}
