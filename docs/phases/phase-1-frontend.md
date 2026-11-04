# Phase 1.11: Frontend (event list and detail)

The React app shows real events from Postgres, through the gateway:

```
Browser (5173) → api-gateway (8080) /api/events → event-service (8081) /events → Postgres
```

## Setup

```bash
cd frontend
npm install react-router @tanstack/react-query
cp .env.example .env.local      # optional: only if the gateway isn't on localhost:8080
npm run dev
```

## Structure

```
frontend/src/
├── main.tsx                     # entry: QueryClientProvider + RouterProvider
├── index.css                    # design tokens (CSS variables), light/dark, base styles
├── env.d.ts                     # types for import.meta.env (VITE_API_BASE_URL)
├── config/env.ts                # API base URL, with a localhost default
├── api/
│   ├── http.ts                  # getJson() + ApiError: the only place that calls fetch
│   └── queryClient.ts           # TanStack Query defaults (cache time, retry policy)
├── routes/router.tsx            # all routes in one place
├── components/                  # app-wide UI: Layout, Icon, StatusMessage, RouteErrorPage
├── pages/                       # one component per route: list, detail, not found
└── features/events/             # everything about events
    ├── types.ts                 # TypeScript shape of the API's EventResponse
    ├── api.ts                   # fetchEvents(), fetchEvent(id)
    ├── queries.ts               # query keys, queryOptions, useEvents(), useEvent()
    ├── format.ts                # date formatting, status labels, id parsing
    └── components/              # EventCard, EventCover, DateTile, StatusBadge, skeleton
```

## Routes

| Path | Page | Data |
|---|---|---|
| `/` | `EventListPage` | `GET /api/events` |
| `/events/:eventId` | `EventDetailPage` | `GET /api/events/{id}` |
| anything else | `NotFoundPage` | none |

## Decisions

### Layers: page → hook → api → http
Each layer has one job. Pages render UI states. Hooks (`useEvents`) connect to the cache. `api.ts` knows the URLs. `http.ts` knows `fetch`, errors and the base URL. Changing the API base URL, adding the Keycloak token (Phase 2), or switching to generated clients each touches one file.

### TanStack Query instead of `useEffect` + `useState`
Hand-written fetching needs loading, error, race-condition, cancellation and caching code in every component. TanStack Query provides it:
- **Caching:** going back from a detail page shows the list instantly, without a spinner (`staleTime: 30s`).
- **Cancellation:** the `signal` is passed to `fetch`, so leaving a page aborts its request.
- **Retry policy:** network errors and 5xx retry twice. **4xx never retries**, because a 404 won't change by asking again.
- **Query key factory (`eventKeys`):** one place defines cache keys. Phase 1's write side will invalidate `eventKeys.all` after creating or publishing an event.

### Feature folders
Code is grouped by feature (`features/events`), not by file type. When booking and tickets arrive, they get their own folders, and nothing becomes a 50-file `components/` folder.

### Types mirror the backend DTO
`EventSummary` matches `EventResponse.java`. `status` is a union type (`'DRAFT' | 'PUBLISHED' | 'CANCELLED'`), not a TypeScript `enum`: the template's `erasableSyntaxOnly` setting forbids enums, and unions cost nothing at runtime. Dates stay ISO strings from the API and are formatted only for display.
Later option: generate these types from an OpenAPI spec (springdoc) so the frontend can't drift from the backend.

### Errors as data
`ApiError` carries the HTTP status. Pages decide what it means: 404 shows "Event not found", 0 (network) explains the gateway may be down, and anything else shows a retry button. Invalid ids (`/events/abc`) are rejected before any request is made.

### Dates
`Intl.DateTimeFormat` formats in the **viewer's** locale and time zone. The backend sends UTC (`Instant`), and the browser converts it. No date library is needed. The `<time dateTime>` element keeps the machine-readable value for accessibility and search engines.

### Styling: CSS Modules + design tokens
No UI library or Tailwind yet. CSS Modules (`*.module.css`) are built into Vite and scope class names to their component, so styles can't collide. Colors, radii, shadows and spacing are CSS variables in `index.css`, with a dark theme through `prefers-color-scheme`. Switching to Tailwind or a component library later only replaces the CSS files.

### Visual design
- **Generated covers (`EventCover`):** events have no images yet, so each gets a gradient whose colors are derived from its id. The same event always gets the same colors, with no image storage or loading. When real images arrive, only this component changes.
- **Date tile (`DateTile`):** month and day are the first thing people scan when picking an event, so they sit on the cover.
- **Status is shown three ways:** a colored badge, a muted (grayscale) cover for cancelled events, and a ticket panel message on the detail page. Color is never the only signal.
- **Icons are inline SVG (`components/Icon.tsx`):** a handful of icons doesn't justify an icon library. They inherit text color and are hidden from screen readers.
- **Fonts:** Inter / Inter Tight from Google Fonts, falling back to the system font if blocked.

### Filters live in the URL
The status chips write `?status=PUBLISHED` to the address bar (`useSearchParams`), instead of React state. Reloading, sharing a link, or pressing Back keeps the filter. They filter data already loaded; the public-vs-draft rule still belongs in the backend.

### Accessibility
Semantic HTML (`main`, `header`, `article`, `ul`, `dl`, `time`), loading regions with `aria-busy`, errors announced with `role="alert"`, visible focus outlines, and whole-card click targets that are still a single real link (one tab stop per card). Skeleton animation respects `prefers-reduced-motion`.

### Page titles
React 19 hoists a `<title>` rendered inside a component into `<head>`, so each page sets its own title without a helper library.

### Configuration
The API base URL comes from `VITE_API_BASE_URL` (see `.env.example`), with `http://localhost:8080` as the default. Only variables prefixed with `VITE_` reach the browser bundle, so secrets must never go there.

## Known limits (planned)
- The list shows **all** events, including drafts. Filtering public events belongs in the **backend** (a public endpoint returns only `PUBLISHED`; organizers see drafts after Phase 2 adds roles).
- No automated tests yet: Vitest + Testing Library, and Playwright end-to-end, come in Phase 7.
- No seat information yet: event-service will expose seats with prices in a later step, and availability comes from booking-service in Phase 3.
