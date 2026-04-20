# Event API Test Scenarios

This file summarizes the scenarios covered by `GIVEN/event-api-test.http`.

## Auth
1. **Auth login (admin)** — `POST /api/v1/auth/login`
   - Expected: `200 OK` with `accessToken`.
   - Token is captured into `@adminToken` for subsequent requests.

## Event Management (Admin)
2. **Create event (happy path)** — `POST /api/v1/admin/events`
   - Expected: `201 Created`, status `INACTIVE`.
3. **Create event (no description)** — `POST /api/v1/admin/events`
   - Expected: `201 Created`, description omitted/null.
4. **Create event (missing required fields)** — `POST /api/v1/admin/events`
   - Expected: `400 ValidationFailed` with field errors.
5. **Create event (past eventDateTime)** — `POST /api/v1/admin/events`
   - Expected: `400 ValidationFailed` on `eventDateTime`.
6. **Create event (empty body)** — `POST /api/v1/admin/events`
   - Expected: `400 ValidationFailed` or `400 HttpMessageNotReadable`.
7. **Create event (name too long)** — `POST /api/v1/admin/events`
   - Expected: `400 ValidationFailed` on `name`.
8. **Update event (happy path)** — `PUT /api/v1/admin/events/{eventId}`
   - Expected: `200 OK`, fields updated.
9. **Update event (non-existent ID)** — `PUT /api/v1/admin/events/{eventId}`
   - Expected: `404 ResourceNotFound`.
10. **Update event (invalid body)** — `PUT /api/v1/admin/events/{eventId}`
    - Expected: `400 ValidationFailed`.
11. **Activate event (INACTIVE → ACTIVE)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `200 OK`.
12. **Activate event (already ACTIVE)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `409 EventAlreadyActive`.
13. **Deactivate event (ACTIVE → INACTIVE)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `200 OK`.
14. **Deactivate event (already INACTIVE)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `409 EventAlreadyInactive`.
15. **Update status (invalid status)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `400 ValidationFailed` or `400 HttpMessageNotReadable`.
16. **Update status (non-existent ID)** — `PATCH /api/v1/admin/events/{eventId}/status`
    - Expected: `404 ResourceNotFound`.
17. **Delete event (happy path)** — `DELETE /api/v1/admin/events/{eventId}`
    - Expected: `204 No Content` (soft delete).
18. **Delete event (non-existent ID)** — `DELETE /api/v1/admin/events/{eventId}`
    - Expected: `404 ResourceNotFound`.
19. **Delete event (with confirmed bookings)** — `DELETE /api/v1/admin/events/{eventId}`
    - Expected: `204 No Content` (soft delete still allowed).

## Event Browsing (Admin or Customer)
20. **List active events (default pagination)** — `GET /api/v1/events`
    - Expected: `200 OK`, `limit=25`, `offset=0`, `totalCount` present.
21. **List active events (custom pagination)** — `GET /api/v1/events?limit=5&offset=0`
    - Expected: `200 OK`, `limit=5`.
22. **List active events (page 2)** — `GET /api/v1/events?limit=5&offset=5`
    - Expected: `200 OK`, `offset=5`.
23. **List active events (sort by name)** — `GET /api/v1/events?sort=name,asc`
    - Expected: `200 OK`, sorted A→Z.
24. **List active events (sort by eventDateTime desc)** — `GET /api/v1/events?sort=eventDateTime,desc`
    - Expected: `200 OK`, latest first.
25. **List active events (limit clamped)** — `GET /api/v1/events?limit=9999`
    - Expected: `200 OK`, `limit=100`.
26. **List active events (empty result)** — `GET /api/v1/events?offset=100000`
    - Expected: `200 OK`, empty `items`.
27. **Get event by ID (ACTIVE)** — `GET /api/v1/events/{eventId}`
    - Expected: `200 OK`.
28. **Get event by ID (INACTIVE)** — `GET /api/v1/events/{eventId}`
    - Expected: `404 ResourceNotFound`.
29. **Get event by ID (non-existent)** — `GET /api/v1/events/{eventId}`
    - Expected: `404 ResourceNotFound`.

