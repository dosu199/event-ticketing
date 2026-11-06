/**
 * TEMPORARY identity until Keycloak login (Phase 2).
 *
 * A random id per browser, sent as the X-User-Id header so booking-service knows whose
 * hold is whose. It is not authentication: anyone could send any id. Phase 2 replaces it
 * with the logged-in user's token.
 */
const STORAGE_KEY = 'event-ticketing.guest-id'

let memoryId: string | null = null

function newId(): string {
  return crypto.randomUUID()
}

export function getGuestId(): string {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) return stored
    const id = newId()
    localStorage.setItem(STORAGE_KEY, id)
    return id
  } catch {
    // Storage blocked (private mode, settings): keep an id for this page session only.
    memoryId ??= newId()
    return memoryId
  }
}
