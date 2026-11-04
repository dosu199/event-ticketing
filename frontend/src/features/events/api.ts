import { getJson } from '../../api/http'
import type { PageResponse } from '../../api/types'
import type { EventStatus, EventSummary } from './types'

// 24 fills complete rows in 2-, 3- and 4-column layouts.
export const EVENTS_PAGE_SIZE = 24

export interface EventListParams {
  status?: EventStatus
}

export function fetchEventsPage(
  params: EventListParams,
  page: number,
  signal?: AbortSignal,
): Promise<PageResponse<EventSummary>> {
  const query = new URLSearchParams({ page: String(page), size: String(EVENTS_PAGE_SIZE) })
  if (params.status) query.set('status', params.status)
  return getJson<PageResponse<EventSummary>>(`/api/events?${query}`, signal)
}

export function fetchEvent(id: number, signal?: AbortSignal): Promise<EventSummary> {
  return getJson<EventSummary>(`/api/events/${id}`, signal)
}
