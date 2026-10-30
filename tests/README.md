# tests

- `load/`: k6 scripts. Key scenario: 500 virtual users try to book the same 10 seats, and exactly 10 bookings succeed.
- `e2e/`: Playwright tests for the full flow (login → pick seats → pay → see ticket).

Unit and integration tests live inside each service's `src/test`.
