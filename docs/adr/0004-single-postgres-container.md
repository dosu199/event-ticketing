# 0004: One Postgres container, one database per service

- **Status:** accepted
- **Date:** 2026-10-30

## Context
"Database per service" is a core microservices rule: a service never reads another service's tables. Strictly, each service would have its own database server.

## Options
1. One Postgres **container** per service (4 containers)
2. One Postgres container holding **separate databases and users** per service

## Decision
**Option 2.**

## Why
- It saves roughly 100–200 MB of RAM per avoided container, which matters in a Codespace.
- The isolation that matters is preserved: each service gets its **own database and its own user**, and that user can only access its own database. booking-service *cannot* query `event_db`, even by accident.
- In production you could move any database to its own server by changing only that service's connection URL.

## Consequences
- If the Postgres container goes down, all services lose their database at once. Locally that's acceptable.
- Databases are created by an init script that runs **only the first time** the data volume is created. To add a database later, either reset the volume (`make reset`) or create it by hand.
