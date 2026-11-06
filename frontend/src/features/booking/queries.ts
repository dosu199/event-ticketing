import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createHold, fetchAvailability, releaseHold } from './api'
import type { Availability } from './types'

/** Until live updates arrive (Phase 5: server-sent events), poll for other people's holds. */
const AVAILABILITY_POLL_MS = 5_000

export const bookingKeys = {
  availability: (eventId: number) => ['booking', 'availability', eventId] as const,
}

export function useAvailability(eventId: number, enabled: boolean) {
  return useQuery({
    queryKey: bookingKeys.availability(eventId),
    queryFn: ({ signal }) => fetchAvailability(eventId, signal),
    enabled,
    staleTime: 0,
    refetchInterval: AVAILABILITY_POLL_MS,
  })
}

export function useCreateHold(eventId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (seatIds: number[]) => createHold(eventId, seatIds),
    onSuccess: (hold) => {
      // Show the new hold immediately, then confirm with a fresh fetch.
      queryClient.setQueryData<Availability>(bookingKeys.availability(eventId), (old) =>
        old ? { ...old, myHold: hold } : old,
      )
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: bookingKeys.availability(eventId) }),
  })
}

export function useReleaseHold(eventId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (holdId: string) => releaseHold(holdId),
    onSuccess: () => {
      queryClient.setQueryData<Availability>(bookingKeys.availability(eventId), (old) =>
        old ? { ...old, myHold: null } : old,
      )
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: bookingKeys.availability(eventId) }),
  })
}
