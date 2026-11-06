import { infiniteQueryOptions, keepPreviousData, queryOptions, useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { fetchEvent, fetchEventsPage, fetchSeats, type EventListParams } from './api'

export const eventKeys = {
  all: ['events'] as const,
  lists: () => [...eventKeys.all, 'list'] as const,
  list: (params: EventListParams) => [...eventKeys.lists(), params] as const,
  detail: (id: number) => [...eventKeys.all, 'detail', id] as const,
  seats: (id: number) => [...eventKeys.detail(id), 'seats'] as const,
}

export const eventsInfiniteQuery = (params: EventListParams) =>
  infiniteQueryOptions({
    queryKey: eventKeys.list(params),
    queryFn: ({ pageParam, signal }) => fetchEventsPage(params, pageParam, signal),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
    // While a new filter loads, keep showing the previous results instead of a skeleton.
    placeholderData: keepPreviousData,
  })

export const eventQuery = (id: number) =>
  queryOptions({
    queryKey: eventKeys.detail(id),
    queryFn: ({ signal }) => fetchEvent(id, signal),
  })

export function useEventsInfinite(params: EventListParams) {
  return useInfiniteQuery(eventsInfiniteQuery(params))
}

export function useEvent(id: number | null) {
  return useQuery({ ...eventQuery(id ?? 0), enabled: id !== null })
}

export const seatsQuery = (eventId: number) =>
  queryOptions({
    queryKey: eventKeys.seats(eventId),
    queryFn: ({ signal }) => fetchSeats(eventId, signal),
  })

export function useSeats(eventId: number | null) {
  return useQuery({ ...seatsQuery(eventId ?? 0), enabled: eventId !== null })
}
