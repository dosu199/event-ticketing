/** Must match booking.hold.max-seats in booking-service (the server enforces it too). */
export const MAX_SEATS_PER_HOLD = 6

/** Under this many milliseconds left, the countdown turns orange. */
export const HOLD_URGENT_MS = 2 * 60 * 1000
