import { API_BASE_URL } from '../config/env'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }

  get isNotFound(): boolean {
    return this.status === 404
  }

  get isNetworkError(): boolean {
    return this.status === 0
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object') {
      const record = body as Record<string, unknown>
      for (const key of ['detail', 'message', 'error']) {
        if (typeof record[key] === 'string') return record[key]
      }
    }
  } catch {
    // body was empty or not JSON
  }
  return `Request failed with status ${response.status}`
}

export async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { Accept: 'application/json' },
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, "Can't reach the server. Is the API gateway running?")
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response))
  }
  return (await response.json()) as T
}
