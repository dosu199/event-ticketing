# Phase 3 (part 1): Seat selection and holds

Users pick seats on the plan and reserve them for 10 minutes. Nobody else can take reserved seats, and two users can never reserve the same seat. Payment and confirmed bookings follow in Phase 4.

> Built before Phase 2 (login). A **temporary guest id** stands in for the user until Keycloak is connected.

```
React ──► api-gateway /api/booking/** ──► booking-service (8082) ──► Redis
                                               │
                                               └── REST ──► event-service (event status, seat list)
```

## booking-service API

Through the gateway, prefix with `/api/booking`. Every call sends `X-User-Id: <guest id>` (temporary).

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/events/{eventId}/availability` | 200 `{ eventId, unavailableSeatIds, myHold }` | |
| POST | `/holds` `{ eventId, seatIds }` | 201 hold + `Location` | 400 invalid / not on sale / too many seats / seat not in event, 404 unknown event, 409 seats taken (`unavailableSeatIds`) or already holding, 503 event-service down |
| GET | `/holds/{holdId}` | 200 hold | 404 (also for someone else's hold) |
| DELETE | `/holds/{holdId}` | 204 | 404 |

Example:

```bash
curl -s -X POST localhost:8080/api/booking/holds \
  -H "Content-Type: application/json" -H "X-User-Id: alice" \
  -d '{"eventId": 1, "seatIds": [1, 2]}'

curl -s localhost:8080/api/booking/events/1/availability -H "X-User-Id: bob"
```

## Rules
- Only **PUBLISHED** events can be booked; seats must belong to the event (checked with event-service).
- Up to **6 seats** per hold (`booking.hold.max-seats`).
- **One active hold per user per event.** Release it to pick different seats.
- **All-or-nothing:** if any requested seat is taken, nothing is held, and the response lists the taken seats.
- Holds expire after **10 minutes** (`booking.hold.ttl`) and free their seats automatically.

## How it works

### Atomic holds with Lua (ADR 0006)
`hold-seats.lua` runs inside Redis as one uninterruptible step:
1. If the user already holds seats for the event, return `ALREADY_HOLDING`.
2. Check every requested seat; if any is held, return `TAKEN` + those seat ids.
3. Otherwise store `seatId → holdId` for each seat, each with its own expiry, plus the hold details and the user index.

Because no other command can run between the check and the write, the classic race ("both users see the seat free, both take it") cannot happen.

### Expiry without cleanup jobs
Each seat entry, the hold record and the user index get the same TTL. When it passes, Redis removes them. Availability reads simply don't see expired seats.

### Releasing safely
`release-hold.lua` only deletes seats that still point to **this** hold, so a late release can never free a seat that expired and was taken by someone else meanwhile.

### Talking to event-service
booking-service calls `GET /events/{id}` and `/events/{id}/seats` with `RestClient`:
- explicit **timeouts** (2 s connect, 3 s read), so a slow event-service can't hang booking-service
- 404 → "event not found"; any other failure → **503** with a friendly message
- booking-service keeps its own small copies of the response shapes (`CatalogEvent`, `CatalogSeat`) and ignores unknown fields, so event-service can evolve independently
- Phase 6 adds retries and a circuit breaker

### Temporary identity
`X-User-Id` is a random id the browser generates once and keeps in localStorage. It's **not security**: anyone can send any id. It's validated (letters, digits, `-`, max 64) because it becomes part of Redis keys. Phase 2 replaces it with the user id from a verified Keycloak token.

## Frontend

- **Seat picker** (`features/booking/components/SeatPicker.tsx`) replaces the static plan for published events. Each seat is a real button with a full label ("Row A, seat 4, VIP, €89.99, available") and states: available (outlined in category color), selected, reserved for you (green), taken (striped, disabled).
- **Reserve** sends the selection; while reserved, a **countdown** shows the remaining time (orange under 2 minutes), with **Release seats**. "Continue to payment" waits for Phase 4.
- **Conflicts:** if someone takes a seat first, the 409 response names it; the picker drops only that seat and keeps the rest selected, with a clear message.
- **Availability polling every 5 s** shows other people's holds. Phase 5 replaces polling with live server-sent events.
- **Graceful degradation:** if booking-service is down, the plan and prices still show, with a notice that selection is temporarily unavailable.
- Totals are summed in cents to avoid floating-point errors.

## Tests (booking-service)

```bash
./mvnw -pl services/booking-service verify     # needs Docker (Redis container)
```

- Real **Redis 7.4** in Docker (Testcontainers), event-service replaced by an in-memory fake.
- **50 users race for the same seat at the same instant → exactly 1 wins, 49 get "taken".**
- All-or-nothing, one hold per user, release, someone else's hold is invisible, invalid requests, expiry (1-second TTL), HTTP status codes and Problem Details.

## Known limits (next steps)
- No login yet: the guest id is not authentication (Phase 2).
- No payment or confirmed bookings: holds just expire (Phase 4: booking in PostgreSQL, payment saga over Kafka; sold seats then also count as unavailable).
- Polling instead of live updates (Phase 5).
- The countdown uses the browser's clock against the server's `expiresAt`; a badly wrong device clock shifts the display (the server still enforces the real expiry).
