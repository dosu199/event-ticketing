# ticket-service  (port 8084)

Issues tickets for confirmed bookings and validates them at the venue entrance.

## Owned data (Postgres: `ticket_db`)
- `ticket`: bookingId, seat, userId, code, status (VALID/USED/REVOKED)

## Planned API
| Method | Path | Notes |
|---|---|---|
| GET | `/tickets/me` | My tickets |
| GET | `/tickets/{id}/qr` | QR code image |
| POST | `/tickets/{code}/validate` | Entrance scan (ORGANIZER) |

## Events
- Consumes: `BookingConfirmed`, `BookingCancelled`
- Publishes: `TicketIssued`

## Checklist
- [ ] Consume `BookingConfirmed` and create tickets
- [ ] QR generation (ZXing)
- [ ] Validation endpoint (a ticket can be used only once)
