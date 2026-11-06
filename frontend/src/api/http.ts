import { API_BASE_URL } from '../config/env'

/** RFC 9457 Problem Details body, as returned by our services on errors. */
export type ProblemBody = Record<string, unknown> & {
  status?: number
  title?: string
  detail?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemBody | null

  constructor(status: number, message: string, problem: ProblemBody | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  get isNotFound(): boolean {
    return this.status === 404
  }

  get isConflict(): boolean {
    return this.status === 409
  }

  get isNetworkError(): boolean {
    return this.status === 0
  }
}

async function readProblem(response: Response): Promise<ProblemBody | null> {
  try {
    const body: unknown = await response.json()
    return body && typeof body === 'object' ? (body as ProblemBody) : null
  } catch {
    return null // empty or not JSON
  }
}

function messageFrom(problem: ProblemBody | null, status: number): string {
  for (const key of ['detail', 'message', 'error', 'title'] as const) {
    const value = problem?.[key]
    if (typeof value === 'string' && value) return value
  }
  return `Request failed with status ${status}`
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  headers?: Record<string, string>
  signal?: AbortSignal
}

/** The only place that calls fetch: base URL, JSON, and error translation. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, headers = {}, signal } = options
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      signal,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, "Can't reach the server. Is the API gateway running?")
  }

  if (!response.ok) {
    const problem = await readProblem(response)
    throw new ApiError(response.status, messageFrom(problem, response.status), problem)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { signal })
}
