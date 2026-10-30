# infra

| Folder | Contents |
|---|---|
| `docker/` | `docker-compose.yml` for local dev (use profiles: `infra`, `services`, `observability`) |
| `keycloak/` | Realm export imported on Keycloak startup |
| `k8s/` | Kustomize base + local overlay (Phase 8) |
| `helm/` | Helm chart (Phase 8) |
| `observability/` | Prometheus scrape config, Grafana dashboards and datasources |

## docker-compose checklist
- [x] postgres, with one DB + user per service via `docker/postgres/init/` (ADR 0004)
- [x] redis
- [x] kafka (KRaft, ADR 0003) + kafka-ui
- [x] keycloak (dev mode)
- [ ] keycloak realm import (Phase 2)
- [x] mailpit
- [ ] prometheus, grafana, zipkin (profile `observability`, Phase 6)
- [ ] services as containers (profile `services`)

Run it with `make up` / `make ps` / `make down` from the repo root. Each container is explained in [docs/phases/phase-0.md](../docs/phases/phase-0.md#5-the-infrastructure-infradockerdocker-composeyml), and the compose file itself is commented line by line.
