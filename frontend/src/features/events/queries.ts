import { queryOptions, useQuery } from '@tanstack/react-query'
import { fetchEvent, fetchEvents } from './api'

export const eventKeys = {
  all: ['events'] as const,
  list: () => [...eventKeys.all, 'list'] as const,
  detail: (id: number) => [...eventKeys.all, 'detail', id] as const,
}

export const eventsQuery = () =>
  queryOptions({
    queryKey: eventKeys.list(),
    queryFn: ({ signal }) => fetchEvents(signal),
  })

export const eventQuery = (id: number) =>
  queryOptions({
    queryKey: eventKeys.detail(id),
    queryFn: ({ signal }) => fetchEvent(id, signal),
  })

export function useEvents() {
  return useQuery(eventsQuery())
}

export function useEvent(id: number | null) {
  return useQuery({ ...eventQuery(id ?? 0), enabled: id !== null })
}
