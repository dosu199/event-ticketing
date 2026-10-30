# Event catalog

| Event | Producer | Consumers | Topic | Payload |
|---|---|---|---|---|
| SeatsHeld | booking | notification | | |
| SeatsReleased | booking | notification | | |
| BookingCreated | booking | | | |
| PaymentRequested | booking | payment | | |
| PaymentSucceeded | payment | booking | | |
| PaymentFailed | payment | booking | | |
| BookingConfirmed | booking | ticket, notification | | |
| BookingCancelled | booking | ticket, notification | | |
| TicketIssued | ticket | notification | | |
| EventPublished | event | | | |
| EventCancelled | event | booking, notification | | |
