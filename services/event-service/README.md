# event-service  (port 8081)

Owns the event catalog: events, venues, seat layouts, prices.

> **Current state (Phase 0):** skeleton only. It starts and answers `GET /actuator/health`. See [Phase 0 guide](../../docs/phases/phase-0.md).

## Owned data (Postgres: `event_db`)
- `venue`: name, address, seating layout (rows/seats or sections)
- `event`: title, description, venue, start time, status (DRAFT/PUBLISHED/CANCELLED)
- `seat`: event, row, number, category, price

## Planned API
| Method | Path | Role |
|---|---|---|
| GET | `/events` | public (paging, filtering) |
| GET | `/events/{id}` | public |
| GET | `/events/{id}/seats` | public |
| POST | `/events` | ORGANIZER |
| PUT | `/events/{id}` | ORGANIZER |
| POST | `/events/{id}/publish` | ORGANIZER |

## Events
- Publishes: `EventPublished`, `EventCancelled`
- Consumes: nothing initially

## Checklist
- [ ] Entities and Flyway `V1__init.sql`
- [ ] Seed data migration
- [ ] Repositories and service layer
- [ ] Controllers, DTOs, validation, error handling
- [ ] Security (roles)
- [ ] Integration test with Testcontainers
