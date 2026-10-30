# payment-service  (port 8083)

Simulates a payment provider. Later this can be swapped for Stripe test mode.

> **Current state (Phase 0):** skeleton only. It starts and answers `GET /actuator/health`. See [Phase 0 guide](../../docs/phases/phase-0.md).

## Owned data (Postgres: `payment_db`)
- `payment`: bookingId, amount, status, provider reference, idempotency key

## Events
- Consumes: `PaymentRequested`
- Publishes: `PaymentSucceeded`, `PaymentFailed`

## Checklist
- [ ] Consume `PaymentRequested`
- [ ] Mock provider (configurable success rate and delay)
- [ ] Idempotency (the same request processed twice must not charge twice)
- [ ] Publish result
- [ ] (Stretch) Stripe test mode + webhook
