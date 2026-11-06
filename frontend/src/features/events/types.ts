export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED'

export interface VenueSummary {
  id: number
  name: string
  city: string
}

export interface EventSummary {
  id: number
  title: string
  description: string | null
  startTime: string
  status: EventStatus
  venue: VenueSummary
}

export type SeatCategory = 'STANDARD' | 'VIP'

export interface Seat {
  id: number
  rowLabel: string
  seatNumber: number
  category: SeatCategory
  price: number
}
