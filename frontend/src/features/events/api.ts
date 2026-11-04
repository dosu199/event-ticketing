import { getJson } from '../../api/http'
import type { EventSummary } from './types'

export function fetchEvents(signal?: AbortSignal): Promise<EventSummary[]> {
  return getJson<EventSummary[]>('/api/events', signal)
}

export function fetchEvent(id: number, signal?: AbortSignal): Promise<EventSummary> {
  return getJson<EventSummary>(`/api/events/${id}`, signal)
}
