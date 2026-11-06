# 0006: Seat holds in Redis, made atomic with Lua scripts

- **Status:** accepted
- **Date:** 2026-11-05

## Context
When a user picks seats, they must be reserved for a short time (10 minutes) so nobody else can take them while the user pays. Many users may click the same seat at the same moment; exactly one may win. Abandoned holds must free themselves.

## Options
1. **PostgreSQL table of holds** with a unique constraint on (event, seat) and a scheduled job deleting expired rows
2. **Redis keys per seat** with `SET NX PX` (set-if-absent with expiry), one command per seat
3. **Redis + a Lua script** that checks and sets all seats of a hold in one atomic step

## Decision
**Option 3.**

- Hold data lives in Redis (`booking-service`), with keys:
  `hold:event:{eventId}` (hash seatId → holdId, per-field expiry), `hold:{holdId}` (hold details), `hold:user:{eventId}:{userId}` (one hold per user per event).
- `hold-seats.lua` checks every requested seat, and either takes all of them or none, returning which seats were taken.
- Expiry uses Redis TTLs. Per-field hash expiry (`HPEXPIRE`) needs **Redis 7.4+**, which docker-compose already runs.

## Why not the others
- **Postgres:** a correct option, but every click becomes a write plus row locks on a hot table, and expired holds need a cleanup job. Holds are temporary and don't need durability.
- **`SET NX` per seat:** atomic per seat, but a 3-seat hold could get 2 seats and fail on the third, leaving half a hold to undo, with races during the undo. The Lua script makes the whole hold one operation.

## Consequences
- Easier: correct under concurrency (proven by a 50-thread test), no cleanup jobs, fast reads for availability polling.
- Harder: logic split between Java and Lua; Lua scripts must stay short because Redis runs nothing else while a script runs.
- Holds are lost if Redis restarts. That's acceptable for temporary holds; **confirmed bookings** (Phase 4) go to PostgreSQL.
- In a Redis **Cluster**, keys used by one script must share a hash slot. That needs hash tags (e.g. `hold:{42}:...`) before moving to a cluster.
