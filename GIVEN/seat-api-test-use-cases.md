# Seat API Test Use Cases

This file summarizes the use cases covered by `GIVEN/seat-api-test.http`.

## Auth
1. **Admin login** — `POST /api/v1/auth/login`
   - Expected: `200 OK`, `accessToken` captured into `@adminToken`.

## Setup
2. **Create event for seat tests** — `POST /api/v1/admin/events`
   - Expected: `201 Created`, capture `eventId` from response.

## Seat Management (Admin)
3. **Bulk create seats (happy path)** — `POST /api/v1/admin/events/{eventId}/seats`
   - Expected: `201 Created`, `items[]` returned, capture `seatId`.
4. **Bulk create seats (duplicate seat number)** — `POST /api/v1/admin/events/{eventId}/seats`
   - Expected: `409 Conflict` (`DuplicateSeatNumber`).
5. **Bulk create seats (empty list)** — `POST /api/v1/admin/events/{eventId}/seats`
   - Expected: `400 Bad Request` (`ValidationFailed`).
6. **Update seat (valid update)** — `PUT /api/v1/admin/events/{eventId}/seats/{seatId}`
   - Expected: `200 OK`, updated fields returned.
7. **Update seat (status BOOKED not allowed)** — `PUT /api/v1/admin/events/{eventId}/seats/{seatId}`
   - Expected: `400 Bad Request` (`InvalidSeatStatusUpdate`).
8. **Update seat (invalid seatId)** — `PUT /api/v1/admin/events/{eventId}/seats/{seatId}`
   - Expected: `404 ResourceNotFound`.

## Event Status Prerequisite
9. **Activate event after seats exist** — `PATCH /api/v1/admin/events/{eventId}/status`
   - Expected: `200 OK` (activation allowed once seats exist).

## Seat Browsing (Admin or Customer)
10. **List seats (event inactive)** — `GET /api/v1/events/{eventId}/seats`
    - Expected: `403 Forbidden` (`EventNotActive`).
11. **List seats (all)** — `GET /api/v1/events/{eventId}/seats`
    - Expected: `200 OK`, all seats returned.
12. **List seats (filter by status)** — `GET /api/v1/events/{eventId}/seats?status=AVAILABLE`
    - Expected: `200 OK`, filtered by status.
13. **List seats (filter by category)** — `GET /api/v1/events/{eventId}/seats?category=VIP`
    - Expected: `200 OK`, filtered by category.

