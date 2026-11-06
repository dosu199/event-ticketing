# Progress log

Where the project stands, and what's next. Update it at the end of every session.

---

## Session: 2026-11-05 (later): seat selection

### Done
- **booking-service:** availability + seat holds in Redis, atomic all-or-nothing Lua scripts, 10-minute expiry, one hold per user per event, max 6 seats (ADR 0006)
- Calls event-service with RestClient (timeouts, 404/503 mapping)
- Integration tests with a real Redis, including a **50-user race for one seat: exactly one wins**
- Gateway route `/api/booking/**`; `make dev` starts booking-service too
- Frontend: clickable seat picker, reserve/release, countdown, conflict handling, polling every 5 s
- Docs: `docs/phases/phase-3-seat-holds.md`, ADR 0006
- Temporary guest id (`X-User-Id`) until Keycloak login

### Next session: start here
1. `./mvnw -pl services/booking-service verify` → all tests pass
2. `make dev`, open an event in two browsers (one normal, one incognito) and race for a seat
3. Choose next: **Phase 2 (Keycloak login, replaces the guest id)** or **Phase 4 (payment + confirmed bookings via Kafka)**

---

## Session: 2026-11-05: Phase 1 complete

### Done
- **Error handling:** `GlobalExceptionHandler` returns Problem Details (RFC 9457) for 400/404/409/500 (ADR 0005)
- **Write side:** `POST /events` (validated), `POST /events/{id}/publish`, `POST /events/{id}/cancel`, lifecycle rules in the `Event` entity
- **Seats:** `GET /events/{id}/seats`; detail page shows prices per category and a seating plan
- **Integration tests:** Testcontainers PostgreSQL + MockMvc (`EventApiIntegrationTest`)
- **Docs:** `docs/phases/phase-1.md`, ADR 0005
- Earlier in Phase 1: schema + seed data, entities, paged and filtered list, gateway route + CORS, frontend list/detail with redesign, `make dev`

### Next session: start here
1. Run `./mvnw -pl services/event-service verify` and confirm all tests pass
2. Try the new endpoints through the gateway (see "API reference" in `docs/phases/phase-1.md`)
3. Decide the order for the housekeeping items below, or start **Phase 2: Keycloak login and roles**

### Housekeeping (any time)
- [ ] Spring profiles: `show-sql` only in dev; demo data (`V3`) only in dev
- [ ] Rebuild the Codespace for Java 21 (currently Java 25)
- [ ] Replace Keycloak's temporary `admin` with a permanent admin user
- [ ] Add the Keycloak/localhost and TypeScript notes to the `docs/phases/phase-0.md` troubleshooting section

### Phase 2 preview
- Keycloak realm `event-ticketing`: roles USER / ORGANIZER / ADMIN, test users, clients for frontend and backend, exported to `infra/keycloak/`
- Gateway and event-service validate JWTs; only ORGANIZER can create, publish, cancel
- Public list shows only PUBLISHED events; organizers also see drafts
- React login/logout, organizer pages (create event form using the validation errors, publish/cancel buttons)

---

## Daily start

```bash
# 0. Open the Codespace in VS Code desktop (Ctrl+Shift+P → Codespaces: Open in VS Code Desktop)
make dev          # infrastructure + event-service + booking-service + api-gateway + frontend; Ctrl+C stops
```

| What | URL |
|---|---|
| Frontend | http://localhost:5173 |
| API via gateway | http://localhost:8080/api/events |
| event-service (direct) | http://localhost:8081/events |
| booking-service (direct) | http://localhost:8082/actuator/health |
| Keycloak (admin / admin) | http://localhost:8180 |
| Mailpit | http://localhost:8025 |
| Kafka UI | http://localhost:8090 |

DBeaver: admin connection `postgres`/`postgres` on db `postgres`; service connection `event_user`/`secret` on db `event_db`.

## Daily stop
Ctrl+C in the `make dev` terminal, `make down` to stop containers, then stop the Codespace on github.com/codespaces.

## Lessons learned
- Use `http://localhost:<port>` (VS Code desktop), not `*.app.github.dev` URLs, especially for Keycloak and CORS.
- Never edit a Flyway migration after it has run. Add a new version, or `make reset` during development.
- Read the **last `Caused by:`** in a stack trace first.
- A service uses its own database user, never the `postgres` superuser.
- Return DTOs from controllers, never entities.
- `@EntityGraph` avoids N+1 for to-one relations; paginated lists need a unique tie-breaker in the sort.
- CORS lives only at the gateway, with exact origins. A 403 on API calls usually means the page runs on a different origin (port!).
- Business rules belong in the entity; the controller only translates HTTP.
- One error format (Problem Details) for every error, and never leak stack traces to clients.
- Check-then-act races need one atomic step: a Lua script in Redis (or a DB constraint/lock), never two separate calls.
- Calls to other services need timeouts, and a down dependency should degrade the UI, not break it.
