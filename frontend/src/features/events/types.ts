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
