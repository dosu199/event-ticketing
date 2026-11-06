export interface Hold {
  holdId: string
  eventId: number
  seatIds: number[]
  expiresAt: string
}

export interface Availability {
  eventId: number
  /** Seats held by other people. */
  unavailableSeatIds: number[]
  /** This browser's active hold for the event, if any. */
  myHold: Hold | null
}
