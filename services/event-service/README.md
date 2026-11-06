# event-service  (port 8081)

Owns the event catalog: venues, events, seats and prices.

> **Current state (end of Phase 1):** full read/write API with validation, lifecycle rules,
> Problem Details errors, pagination, and Testcontainers integration tests.
> Details: [docs/phases/phase-1.md](../../docs/phases/phase-1.md).

## Data (PostgreSQL `event_db`, user `event_user`)
- `venue`: name, city, address
- `event`: venue, title, description, start time, status (`DRAFT` / `PUBLISHED` / `CANCELLED`)
- `seat`: event, row, number, category (`STANDARD` / `VIP`), price

Schema is owned by Flyway (`src/main/resources/db/migration`); Hibernate only validates it.

## API
| Method | Path | Notes |
|---|---|---|
| GET | `/events?status=&page=&size=` | paged, sorted by start time |
| GET | `/events/{id}` | |
| GET | `/events/{id}/seats` | ordered by row and number |
| POST | `/events` | creates a DRAFT, 201 + Location |
| POST | `/events/{id}/publish` | DRAFT → PUBLISHED (future events only) |
| POST | `/events/{id}/cancel` | DRAFT/PUBLISHED → CANCELLED |

Errors: Problem Details (`application/problem+json`), see ADR 0005.

## Run / test
```bash
./mvnw -pl services/event-service spring-boot:run   # needs `make up` (Postgres)
./mvnw -pl services/event-service verify            # needs Docker (Testcontainers)
```

## Packages
| Package | Contents |
|---|---|
| `api` | `EventController`, request/response records, `GlobalExceptionHandler` |
| `service` | `EventService` (use cases, transactions), not-found exceptions |
| `repository` | Spring Data repositories |
| `domain` | Entities, enums, lifecycle rules, `InvalidEventStateException` |
| `config` | `ClockConfig` |

## Next (Phase 2)
- [ ] Validate JWTs from Keycloak; ORGANIZER role for create/publish/cancel
- [ ] Public list returns only PUBLISHED events
- [ ] Publish `EventPublished` / `EventCancelled` to Kafka (Phase 4)
