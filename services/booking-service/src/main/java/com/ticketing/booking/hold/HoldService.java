package com.ticketing.booking.hold;

import com.ticketing.booking.catalog.CatalogEvent;
import com.ticketing.booking.catalog.CatalogSeat;
import com.ticketing.booking.catalog.EventCatalogClient;
import com.ticketing.booking.config.BookingProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seat holds, stored in Redis.
 *
 * <p>Why Redis: holds are short-lived and checked constantly. Redis keeps them in memory,
 * expires them automatically (no cleanup job), and runs Lua scripts atomically, which is
 * what prevents two users from holding the same seat.
 */
@Service
public class HoldService {

    private final StringRedisTemplate redis;
    private final EventCatalogClient catalog;
    private final BookingProperties.Hold settings;
    private final Clock clock;
    private final RedisScript<List> holdScript;
    private final RedisScript<Long> releaseScript;

    public HoldService(StringRedisTemplate redis, EventCatalogClient catalog,
                       BookingProperties properties, Clock clock) {
        this.redis = redis;
        this.catalog = catalog;
        this.settings = properties.hold();
        this.clock = clock;
        this.holdScript = new DefaultRedisScript<>(load("scripts/hold-seats.lua"), List.class);
        this.releaseScript = new DefaultRedisScript<>(load("scripts/release-hold.lua"), Long.class);
    }

    /**
     * Holds all requested seats for the user, or none of them.
     *
     * @throws InvalidHoldRequestException if the event isn't on sale, a seat isn't part of
     *                                     the event, or too many seats were requested
     * @throws SeatsUnavailableException   if any seat is already held by someone else
     * @throws AlreadyHoldingException     if the user already holds seats for this event
     */
    public Hold hold(String userId, long eventId, List<Long> requestedSeatIds) {
        List<Long> seatIds = List.copyOf(new LinkedHashSet<>(requestedSeatIds)); // drop duplicates, keep order
        if (seatIds.size() > settings.maxSeats()) {
            throw new InvalidHoldRequestException("You can hold at most " + settings.maxSeats() + " seats at a time");
        }

        // Ask event-service: does the event exist, is it on sale, do these seats belong to it?
        CatalogEvent event = catalog.getEvent(eventId);
        if (!event.isOnSale()) {
            throw new InvalidHoldRequestException("Event " + eventId + " is not on sale");
        }
        Set<Long> eventSeatIds = catalog.getSeats(eventId).stream().map(CatalogSeat::id).collect(Collectors.toSet());
        List<Long> foreign = seatIds.stream().filter(id -> !eventSeatIds.contains(id)).toList();
        if (!foreign.isEmpty()) {
            throw new InvalidHoldRequestException("Seats " + foreign + " don't belong to event " + eventId);
        }

        String holdId = UUID.randomUUID().toString();
        Duration ttl = settings.ttl();
        Instant expiresAt = Instant.now(clock).plus(ttl);

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(ttl.toMillis()));
        args.add(holdId);
        args.add(userId);
        args.add(String.valueOf(eventId));
        args.add(String.valueOf(expiresAt.toEpochMilli()));
        seatIds.forEach(id -> args.add(String.valueOf(id)));

        List<?> result = redis.execute(holdScript,
                List.of(HoldKeys.eventSeats(eventId), HoldKeys.hold(holdId), HoldKeys.userHold(eventId, userId)),
                args.toArray());

        String outcome = result == null || result.isEmpty() ? "" : String.valueOf(result.get(0));
        switch (outcome) {
            case "OK" -> {
                return new Hold(holdId, userId, eventId, seatIds, expiresAt);
            }
            case "ALREADY_HOLDING" -> throw new AlreadyHoldingException(eventId);
            case "TAKEN" -> throw new SeatsUnavailableException(
                    result.subList(1, result.size()).stream().map(v -> Long.valueOf(String.valueOf(v))).toList());
            default -> throw new IllegalStateException("Unexpected hold script result: " + result);
        }
    }

    /** Which seats of the event are held by others, plus the user's own hold (if any). */
    public Availability availability(long eventId, String userId) {
        Map<Object, Object> heldSeats = redis.opsForHash().entries(HoldKeys.eventSeats(eventId));

        Hold myHold = userId == null ? null : findUserHold(eventId, userId);
        String myHoldId = myHold == null ? null : myHold.id();

        Set<Long> unavailable = new HashSet<>();
        heldSeats.forEach((seatId, holdId) -> {
            if (!holdId.equals(myHoldId)) {
                unavailable.add(Long.valueOf(seatId.toString()));
            }
        });
        return new Availability(eventId, unavailable, myHold);
    }

    /** The user's own hold. Someone else's hold is reported as not found. */
    public Hold getHold(String userId, String holdId) {
        Hold hold = readHold(holdId);
        if (hold == null || !hold.userId().equals(userId)) {
            throw new HoldNotFoundException(holdId);
        }
        return hold;
    }

    /** Releases the user's hold immediately (instead of waiting for it to expire). */
    public void release(String userId, String holdId) {
        Hold hold = getHold(userId, holdId);
        List<String> args = new ArrayList<>();
        args.add(holdId);
        hold.seatIds().forEach(id -> args.add(String.valueOf(id)));
        redis.execute(releaseScript,
                List.of(HoldKeys.eventSeats(hold.eventId()), HoldKeys.hold(holdId), HoldKeys.userHold(hold.eventId(), userId)),
                args.toArray());
    }

    private Hold findUserHold(long eventId, String userId) {
        String holdId = redis.opsForValue().get(HoldKeys.userHold(eventId, userId));
        return holdId == null ? null : readHold(holdId);
    }

    private Hold readHold(String holdId) {
        Map<Object, Object> fields = redis.opsForHash().entries(HoldKeys.hold(holdId));
        if (fields.isEmpty()) {
            return null; // never existed, released, or expired
        }
        List<Long> seatIds = Arrays.stream(String.valueOf(fields.get("seatIds")).split(","))
                .map(Long::valueOf)
                .toList();
        return new Hold(
                holdId,
                String.valueOf(fields.get("userId")),
                Long.parseLong(String.valueOf(fields.get("eventId"))),
                seatIds,
                Instant.ofEpochMilli(Long.parseLong(String.valueOf(fields.get("expiresAt")))));
    }

    private static String load(String classpathLocation) {
        try (var in = new ClassPathResource(classpathLocation).getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Can't load " + classpathLocation, e);
        }
    }
}
