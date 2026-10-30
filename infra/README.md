# infra

| Folder | Contents |
|---|---|
| `docker/` | `docker-compose.yml` for local dev (use profiles: `infra`, `services`, `observability`) |
| `keycloak/` | Realm export imported on Keycloak startup |
| `k8s/` | Kustomize base + local overlay (Phase 8) |
| `helm/` | Helm chart (Phase 8) |
| `observability/` | Prometheus scrape config, Grafana dashboards and datasources |

## docker-compose checklist
- [ ] postgres (one DB per service via an init script)
- [ ] redis
- [ ] kafka (or redpanda) + optional UI (kafka-ui / redpanda console)
- [ ] keycloak (+ realm import)
- [ ] mailpit
- [ ] prometheus, grafana, zipkin (profile `observability`)
- [ ] services (profile `services`)
