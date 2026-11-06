# booking-service  (port 8082)

Seat availability and seat holds. Later: bookings and the booking saga.

> **Current state (Phase 3, part 1):** availability + 10-minute holds in Redis, atomic via Lua,
> with a temporary guest id. Details: [docs/phases/phase-3-seat-holds.md](../../docs/phases/phase-3-seat-holds.md),
> decision: [ADR 0006](../../docs/adr/0006-seat-holds-in-redis-with-lua.md).

## Data
- **Redis** (holds, temporary):
  - `hold:event:{eventId}`: hash seatId → holdId, each field with its own expiry
  - `hold:{holdId}`: userId, eventId, seatIds, expiresAt
  - `hold:user:{eventId}:{userId}`: the user's active holdId
- **PostgreSQL `booking_db`**: not used yet (confirmed bookings + outbox in Phase 4)

## API (through the gateway: `/api/booking/...`)
| Method | Path |
|---|---|
| GET | `/events/{eventId}/availability` |
| POST | `/holds` |
| GET | `/holds/{holdId}` |
| DELETE | `/holds/{holdId}` |

## Depends on
- Redis (`make up`)
- event-service on `services.event-service.url` (default `http://localhost:8081`)

## Run / test
```bash
./mvnw -pl services/booking-service spring-boot:run
./mvnw -pl services/booking-service verify     # needs Docker
```

## Checklist
- [x] Redis seat holds (atomic, TTL)
- [x] Availability endpoint
- [x] Concurrency test: many threads, same seat
- [ ] Replace guest id with Keycloak user (Phase 2)
- [ ] Booking entity with `@Version`, state machine (Phase 4)
- [ ] Saga orchestration + compensation, outbox (Phase 4)
