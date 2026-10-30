# notification-service  (port 8085)

Stateless. Turns domain events into user-facing notifications.

## Responsibilities
- Send emails (Mailpit locally)
- Push live seat updates to browsers (SSE or WebSocket/STOMP)

## Events
- Consumes: `SeatsHeld`, `SeatsReleased`, `BookingConfirmed`, `TicketIssued`, `BookingCancelled`

## Checklist
- [ ] Email sender + templates
- [ ] SSE/WebSocket endpoint per event (`/notifications/events/{eventId}/seats`)
- [ ] Broadcast seat status changes
