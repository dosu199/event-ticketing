# notification-service  (port 8085)

Stateless. Turns domain events into user-facing notifications.

> **Current state (Phase 0):** skeleton only. It starts and answers `GET /actuator/health`. See [Phase 0 guide](../../docs/phases/phase-0.md).

## Responsibilities
- Send emails (Mailpit locally)
- Push live seat updates to browsers (SSE or WebSocket/STOMP)

## Events
- Consumes: `SeatsHeld`, `SeatsReleased`, `BookingConfirmed`, `TicketIssued`, `BookingCancelled`

## Checklist
- [ ] Email sender + templates
- [ ] SSE/WebSocket endpoint per event (`/notifications/events/{eventId}/seats`)
- [ ] Broadcast seat status changes
