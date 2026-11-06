# 0005: Problem Details (RFC 9457) for all API errors

- **Status:** accepted
- **Date:** 2026-11-05

## Context
Errors came back in different shapes: Spring's default error JSON, the Whitelabel page, or raw messages. Clients (our React app, later other services) need one predictable format to show messages and react to status codes.

## Options
1. Our own error format, e.g. `{ "code": ..., "message": ... }`
2. **RFC 9457 Problem Details** (`application/problem+json`), which Spring supports natively through `ProblemDetail` and `ResponseEntityExceptionHandler`
3. Leave Spring Boot's default error responses

## Decision
**Option 2**, through one `@RestControllerAdvice` (`GlobalExceptionHandler`) per service.

| Situation | Status |
|---|---|
| Invalid input (validation, malformed JSON, bad query params, unknown venue in body) | 400 |
| Resource doesn't exist | 404 |
| Business rule violated (e.g. publishing a cancelled event) | 409 |
| Anything unexpected | 500, generic message; details only in the server log |

Validation errors add an `errors` object (field → message) for forms.

## Consequences
- One shape everywhere, recognised by tools and libraries, and documented by a standard.
- Spring MVC's own errors (type mismatch, unreadable body, unknown URL) are covered automatically by the parent class.
- Every new service copies the same handler. A shared library could remove that duplication later, at the cost of coupling (see ADR 0001's discussion).
- Errors produced by the **gateway itself** (e.g. a service is down) still use the gateway's default format. Aligning those is a later task.
