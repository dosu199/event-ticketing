# Phase 1: Event catalog, end to end

**Goal:** a complete vertical slice: browser → gateway → event-service → PostgreSQL, with a real API for reading and managing events, consistent errors, and automated tests.

```
React (5173) ──► api-gateway (8080) ──/api/events/**──► event-service (8081) ──► Postgres (event_db)
```

## What was built

| Area | Pieces |
|---|---|
| **Database** | Flyway `V1` schema (venue, event, seat + constraints), `V2` seed data, `V3` 1000 demo events, `V4` indexes for the list query |
| **Domain** | Entities `Venue`, `Event`, `Seat`; enums `EventStatus`, `SeatCategory`; lifecycle rules in `Event.publish()` / `cancel()` |
| **API** | List (paged, filter), get, create, publish, cancel, seats |
| **Errors** | `GlobalExceptionHandler`: Problem Details for 400/404/409/500 (ADR 0005) |
| **Gateway** | Route `/api/events/**` → event-service (`StripPrefix=1`), CORS for `http://localhost:5173` |
| **Frontend** | Event list with status filters and "Load more", detail page with seating plan and prices |
| **Tests** | Integration tests against a real PostgreSQL (Testcontainers) |
| **Tooling** | `make dev` / `scripts/dev.sh` starts everything |

## API reference (event-service)

Through the gateway, prefix every path with `/api` (e.g. `GET /api/events`).

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/events?status=&page=0&size=20` | 200 page of events | 400 bad `status`/`page`/`size` |
| GET | `/events/{id}` | 200 event | 404 |
| GET | `/events/{id}/seats` | 200 list of seats (row, number, category, price) | 404 |
| POST | `/events` | 201 + `Location` header, new event (status `DRAFT`) | 400 validation / unknown venue |
| POST | `/events/{id}/publish` | 200 event (`PUBLISHED`) | 404, 409 not a draft or in the past |
| POST | `/events/{id}/cancel` | 200 event (`CANCELLED`) | 404, 409 already cancelled |

### Create an event

```bash
curl -s -i -X POST localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"venueId": 1, "title": "My Concert", "description": "Test", "startTime": "2027-03-01T19:00:00Z"}'
```

| Field | Rules |
|---|---|
| `venueId` | required, must exist |
| `title` | required, not blank, max 255 (surrounding spaces are trimmed) |
| `description` | optional, max 5000 (blank becomes null) |
| `startTime` | required, ISO-8601 instant in the future |

### Error format (Problem Details)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for 2 field(s).",
  "instance": "/events",
  "errors": { "title": "must not be blank", "startTime": "must be a future date" }
}
```

## Event lifecycle

```
DRAFT ──publish──► PUBLISHED
  │                    │
  └──────cancel────────┴──► CANCELLED (final)
```

- Only a `DRAFT` can be published, and only if it starts in the future.
- `DRAFT` or `PUBLISHED` can be cancelled; cancelling twice is a 409.
- The rules live **in the entity** (`Event.publish`, `Event.cancel`), not in the controller or service. Whatever calls them (REST today, a Kafka listener later) gets the same rules.

## Key design decisions

### Layers
`Controller` (HTTP: parameters, status codes, DTOs) → `Service` (use cases, transactions) → `Repository` (queries) → `Entity` (data + business rules). Each layer can be changed or tested without the others.

### DTOs, never entities, in the API
`EventResponse`, `SeatResponse`, `PageResponse`, `CreateEventRequest` are records. Entities stay internal: no lazy-loading surprises during JSON conversion, and the database can change without breaking clients.

### Validation at the edge
`@Valid` + Bean Validation annotations on `CreateEventRequest` reject bad input before any business code runs. Limits mirror the database columns. The database constraints (CHECK, NOT NULL, FK) remain the last line of defense.

### State changes as actions
`POST /events/{id}/publish` instead of `PATCH {"status": "PUBLISHED"}`. The client says what it wants; the server decides whether that's allowed. Each action can get its own permission in Phase 2 (only organizers publish).

### Transactions and dirty checking
Write methods are `@Transactional`. `publish()` loads the event, changes its status, and returns; Hibernate notices the change and writes the `UPDATE` at commit, without an explicit `save()`.

### Time is injected (`Clock` bean)
"Is this event in the past?" depends on now. Injecting a `Clock` instead of calling `Instant.now()` everywhere lets tests use a fixed time.

### Pagination
See [phase-1-pagination.md](phase-1-pagination.md): `Pageable`, stable sort (`startTime`, `id`), filtering in the database, own `PageResponse`, size limit, indexes.

### Seats
`GET /events/{id}/seats` returns seats ordered by row and number, using the unique index on `(event_id, row_label, seat_number)`. It checks the event exists first, so an unknown event is a 404 rather than an empty list. Availability (held/sold) is **not** here; it belongs to booking-service (Phase 3).

## Testing

```bash
./mvnw -pl services/event-service verify     # needs Docker (Testcontainers)
```

- `IntegrationTest` starts **one PostgreSQL container** for the whole run (singleton pattern) and points Spring at it with `@DynamicPropertySource`. Flyway runs all migrations, Hibernate validates the entities: the same path as production.
- `EventApiIntegrationTest` calls the real HTTP layer with MockMvc: paging and filters, 404/400/409 Problem Details, validation field errors, create → get, publish/cancel rules, seats.
- Tests that change data create their own events, so they don't depend on each other's order.
- `make dev` doesn't need to be running: tests use their own throwaway database.

## Known limits (next phases)
- **No authentication yet.** Anyone can create, publish or cancel events, and drafts are visible in the public list. Phase 2 adds Keycloak roles (organizers manage events; the public sees only published ones).
- **No UI for creating or publishing.** That comes with the organizer pages in Phase 2.
- **Demo data runs in every environment.** Moving `V3` to a dev-only Flyway location with Spring profiles is a planned cleanup.
- **`Location` header points to the service** (`localhost:8081/events/5`), not the gateway URL. The gateway can rewrite it later.
- **Prices have no currency field.** The frontend assumes EUR (`config/env.ts`).
