# Event Ticketing Platform

A microservices learning project: users browse events, hold seats, pay, and receive a QR ticket by email.

> **Status:** Phase 0 (setup) done: build, dev environment, and local infrastructure. No business logic yet.
> Each folder has a `README.md` describing what belongs there. Each phase is explained in [`docs/phases/`](docs/phases/).

## Architecture

```
            ┌──────────────┐
 React ───► │ api-gateway  │ ──► Keycloak (JWT)
            └──────┬───────┘
     ┌─────────────┼──────────────┬───────────────┐
     ▼             ▼              ▼               ▼
 event-service  booking-service  ticket-service  notification-service
  (Postgres)    (Postgres+Redis)  (Postgres)      (SSE/WebSocket, email)
                     │
                     ▼
               payment-service (Postgres)

          ◄──────── Kafka (async events) ────────►
```

## Getting started (GitHub Codespaces)

```bash
# once: Ctrl/Cmd+Shift+P → "Codespaces: Rebuild Container" (applies .devcontainer)
mvn -N wrapper:wrapper -Dmaven=3.9.11   # once: generate the Maven wrapper
./mvnw verify                           # build all services + smoke tests
make up && make ps                      # start infrastructure, check health
./mvnw -pl services/event-service spring-boot:run
```

`make help` lists all shortcuts. Full walkthrough: [docs/phases/phase-0.md](docs/phases/phase-0.md).

## Documentation

| Doc | What it covers |
|---|---|
| [docs/phases/](docs/phases/) | One explained guide per roadmap phase: what was added and why |
| [docs/adr/](docs/adr/) | Architecture Decision Records: every significant choice and its alternatives |
| [docs/events.md](docs/events.md) | Event catalog |
| [docs/architecture.md](docs/architecture.md) | Architecture notes |
| `README.md` in each folder | What that folder/service is responsible for |

## Repository layout

| Path | Purpose |
|---|---|
| `services/` | One folder per Spring Boot microservice |
| `libs/event-contracts/` | Shared event schemas (DTOs) published on Kafka |
| `frontend/` | React + Vite + TypeScript app |
| `infra/docker/` | Docker Compose for local development |
| `infra/keycloak/` | Realm export (users, roles, clients) |
| `infra/k8s/`, `infra/helm/` | Kubernetes manifests / Helm chart (later phase) |
| `infra/observability/` | Prometheus + Grafana config |
| `tests/load/`, `tests/e2e/` | k6 load tests, Playwright end-to-end tests |
| `docs/` | Architecture notes, event catalog, ADRs |
| `.github/workflows/` | CI pipelines |
| `.devcontainer/` | Codespaces environment (Java 21, Maven, Node 22, Docker) |
| `pom.xml` | Maven parent: shared versions and module list |
| `Makefile`, `.env.example` | Command shortcuts, and the template for local secrets |

## Tech stack

Java 21 · Spring Boot 4.1 · Spring Cloud 2025.1 · Spring Cloud Gateway · Spring Data JPA · Flyway · Spring Kafka · Resilience4j · PostgreSQL · Redis · Kafka (or Redpanda) · Keycloak · React + Vite + TypeScript · TanStack Query · Docker Compose · Testcontainers · Prometheus/Grafana · k6 · Playwright

## Local ports (planned)

| Component | Port |
|---|---|
| api-gateway | 8080 |
| event-service | 8081 |
| booking-service | 8082 |
| payment-service | 8083 |
| ticket-service | 8084 |
| notification-service | 8085 |
| frontend (Vite) | 5173 |
| Keycloak | 8180 |
| Postgres | 5432 |
| Redis | 6379 |
| Kafka | 9092 (host) / 29092 (inside Docker) |
| Kafka UI | 8090 |
| Mailpit UI / SMTP | 8025 / 1025 |
| Grafana / Prometheus | 3000 / 9090 |

## Roadmap

### Phase 0: Setup ([guide](docs/phases/phase-0.md))
- [x] Build tool: Maven multi-module (ADR 0001), Spring Boot 4.1 / Java 21 (ADR 0002)
- [x] `infra/docker/docker-compose.yml` with Postgres, Redis, Kafka, Kafka UI, Keycloak, Mailpit
- [x] Service skeletons (main class, config, smoke test) for all six services
- [x] Codespaces devcontainer, Makefile, `.env.example`
- [ ] **You:** generate the Maven wrapper, run `./mvnw verify` and `make up`, and check health endpoints

### Phase 1: First vertical slice
- [ ] event-service: CRUD for events/venues, Flyway migration, seed data
- [ ] api-gateway routes `/api/events/**` to event-service
- [ ] Frontend: event list and event detail pages

### Phase 2: Auth
- [ ] Keycloak realm with roles USER / ORGANIZER / ADMIN
- [ ] Gateway and services validate JWTs, and only ORGANIZER can create events
- [ ] Frontend login/logout

### Phase 3: Booking core
- [ ] Seat holds in Redis (`SET NX` + TTL)
- [ ] Bookings in Postgres with optimistic locking (`@Version`)
- [ ] Frontend seat map

### Phase 4: Async and the saga
- [ ] Kafka topics and event contracts
- [ ] Mock payment-service
- [ ] Saga orchestration and compensation in booking-service
- [ ] Transactional outbox

### Phase 5: Tickets and notifications
- [ ] QR ticket generation
- [ ] Confirmation email via Mailpit
- [ ] Live seat updates (SSE/WebSocket)

### Phase 6: Resilience and observability
- [ ] Resilience4j on sync calls
- [ ] Distributed tracing
- [ ] Grafana dashboard
- [ ] Failure drills (kill services mid-flow)

### Phase 7: Quality
- [ ] Testcontainers integration tests
- [ ] k6: 500 users try to book the same 10 seats, and exactly 10 bookings succeed
- [ ] GitHub Actions CI

### Phase 8: Stretch
- [ ] Kubernetes (kind) + Helm
- [ ] Virtual waiting room
- [ ] Organizer sales dashboard
- [ ] Stripe test mode
