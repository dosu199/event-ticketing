# Phase 1: Pagination for the event list

With 1000+ events, `GET /events` returned everything at once: ~350 KB of JSON and 1000 cards rendered.
Now the API returns one page at a time, filtering happens in the database, and the frontend loads more on demand.

## API

```
GET /api/events?status=PUBLISHED&page=0&size=24
```

| Param | Default | Rules |
|---|---|---|
| `status` | (all) | `DRAFT`, `PUBLISHED`, `CANCELLED`. Anything else → 400 |
| `page` | `0` | zero-based, must be ≥ 0, else 400 |
| `size` | `20` | 1–100, else 400 |

Response:

```json
{
  "content": [ { "id": 1, "title": "...", "venue": { ... } } ],
  "page": 0,
  "size": 24,
  "totalElements": 772,
  "totalPages": 33,
  "last": false
}
```

## Backend decisions

- **`Pageable` + `Page<Event>` in the repository.** Spring Data adds `LIMIT/OFFSET` and runs a second `COUNT` query for `totalElements`.
- **`@EntityGraph(venue)` on the paged queries.** Venues come in the same query, so there is no N+1. This is safe with pagination because `venue` is a to-one relation; fetching a to-*many* collection with pagination would make Hibernate paginate in memory.
- **Stable sort: `startTime`, then `id`.** Many events share a start time. Without a unique tie-breaker, the database may order equal rows differently on each query, and an event could show up on two pages or none.
- **Filtering in the database (`findByStatus`).** Filtering in React would give pages with gaps and wrong totals.
- **Own `PageResponse` record instead of returning Spring's `Page`.** Spring's `Page` JSON is an internal format that may change between versions (Spring logs a warning about it). Our record is a stable API contract.
- **Limits on `size` (max 100).** Without a cap, `?size=1000000` brings back the original problem, or worse.
- **Indexes (`V4__index_event_listing.sql`).** `(status, start_time, id)` serves the filtered list and `(start_time, id)` the unfiltered one, so Postgres reads only the rows of the page instead of sorting the whole table.

### Offset vs keyset pagination
We use **offset** pagination (`OFFSET page*size LIMIT size`): simple, standard, gives totals and page numbers. Its cost grows with very deep pages, because the database still walks past the skipped rows. **Keyset** pagination (`WHERE (start_time, id) > (:lastStart, :lastId)`) stays fast at any depth and suits infinite scroll, but can't jump to page N. For this catalog, offset is the right fit.

## Frontend decisions

- **`useInfiniteQuery`** keeps every loaded page in one cache entry. "Load more" fetches the next page using `getNextPageParam` (`last ? undefined : page + 1`).
- **One cache entry per filter** (`eventKeys.list({ status })`). Switching back to a filter you already loaded is instant, with no request.
- **`placeholderData: keepPreviousData`** keeps the old list visible (dimmed) while a new filter loads, instead of flashing skeletons.
- **Filter stays in the URL** (`?status=...`), so reload, share and Back keep it.
- **Page size 24** fills complete rows in 2-, 3- and 4-column layouts.
- **"Load more" button instead of automatic infinite scroll.** It's accessible, keeps the footer reachable, and is easy to swap for an `IntersectionObserver` later.
