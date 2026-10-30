# frontend  (port 5173)

React + Vite + TypeScript.

Bootstrap with: `npm create vite@latest . -- --template react-ts`

## Folder plan
```
src/
├── api/          # HTTP client, API functions per service
├── auth/         # Keycloak/OIDC setup, auth context, protected routes
├── components/   # Shared UI components
├── features/
│   ├── events/     # Event list, details
│   ├── booking/    # Seat map, hold timer, checkout
│   ├── tickets/    # My tickets, QR display
│   └── organizer/  # Create/edit events, sales stats
├── hooks/        # Shared hooks (e.g. useSeatUpdates for SSE)
├── pages/        # Route-level components
├── routes/       # Router config
└── types/        # Shared TypeScript types
```

## Suggested libraries
TanStack Query · React Router · Tailwind · oidc-client-ts or keycloak-js · react-hook-form + zod

## Checklist
- [ ] Bootstrap Vite project
- [ ] Routing and layout
- [ ] Event list and details
- [ ] Login
- [ ] Seat map with hold countdown
- [ ] Checkout and booking status
- [ ] My tickets
- [ ] Live seat updates
- [ ] Organizer pages

## Notes
- Scaffolded with `npm create vite@latest . -- --template react-ts` ("Ignore files and continue").
- TypeScript is pinned to `~6.0.0` because the Vite template's TypeScript 7 conflicts with
  `typescript-eslint` 8.x (peer range `<6.1.0`). Upgrade both together once
  typescript-eslint supports TS 7.
- Run with `npm run dev -- --host` (`--host` is needed in Codespaces).
