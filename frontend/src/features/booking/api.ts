import { request } from '../../api/http'
import { getGuestId } from './guestId'
import type { Availability, Hold } from './types'

const userHeader = () => ({ 'X-User-Id': getGuestId() })

export function fetchAvailability(eventId: number, signal?: AbortSignal): Promise<Availability> {
  return request<Availability>(`/api/booking/events/${eventId}/availability`, { headers: userHeader(), signal })
}

export function createHold(eventId: number, seatIds: number[]): Promise<Hold> {
  return request<Hold>('/api/booking/holds', { method: 'POST', headers: userHeader(), body: { eventId, seatIds } })
}

export function releaseHold(holdId: string): Promise<void> {
  return request<void>(`/api/booking/holds/${holdId}`, { method: 'DELETE', headers: userHeader() })
}
