# booking-service  (port 8082)

The heart of the system. Handles seat holds, bookings, and orchestrates the booking saga.

> **Current state (Phase 0):** skeleton only. It starts and answers `GET /actuator/health`. See [Phase 0 guide](../../docs/phases/phase-0.md).

## Owned data
- **Redis:** seat holds `hold:{eventId}:{seatId}` → userId, TTL 10 min
- **Postgres (`booking_db`):**
  - `booking`: user, event, status, total, `version` (optimistic lock)
  - `booking_seat`: booking, seat
  - `outbox`: events waiting to be published to Kafka

## Booking state machine
```
PENDING ──► AWAITING_PAYMENT ──► CONFIRMED
   │               │
   └──► EXPIRED    └──► CANCELLED (payment failed → release seats)
```

## Planned API
| Method | Path | Notes |
|---|---|---|
| POST | `/bookings/holds` | Hold seats (must not double-book) |
| DELETE | `/bookings/holds/{id}` | Release hold |
| POST | `/bookings` | Create booking from hold, start saga |
| GET | `/bookings/{id}` | Status |
| GET | `/bookings/me` | My bookings |

## Events
- Publishes: `SeatsHeld`, `SeatsReleased`, `BookingCreated`, `PaymentRequested`, `BookingConfirmed`, `BookingCancelled`
- Consumes: `PaymentSucceeded`, `PaymentFailed`

## Checklist
- [ ] Redis seat hold logic (atomic, TTL)
- [ ] Booking entity with `@Version`
- [ ] State machine
- [ ] Saga orchestration and compensation
- [ ] Outbox table and publisher
- [ ] Hold-expiry handling
- [ ] Concurrency test: many threads, same seat
