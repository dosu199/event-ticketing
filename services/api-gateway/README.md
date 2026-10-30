# api-gateway  (port 8080)

Single entry point for the frontend.

> **Current state (Phase 0):** skeleton only. It starts and answers `GET /actuator/health`. See [Phase 0 guide](../../docs/phases/phase-0.md).

## Responsibilities
- Route `/api/<service>/**` to the right service
- Validate JWTs issued by Keycloak
- CORS for the frontend (`localhost:5173`)
- Rate limiting (Redis), especially on booking endpoints
- Propagate trace headers

## Planned routes
| Path | Target |
|---|---|
| `/api/events/**` | event-service |
| `/api/bookings/**` | booking-service |
| `/api/tickets/**` | ticket-service |
| `/api/notifications/**` | notification-service (SSE stream) |

## Checklist
- [ ] Generate project (Gateway, OAuth2 Resource Server, Actuator)
- [ ] Route config in `application.yml`
- [ ] Security config
- [ ] CORS
- [ ] Rate limiter
