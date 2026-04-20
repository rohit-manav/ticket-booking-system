# Optimistic Locking Use Case (Seat Booking / Update)

This document explains why the `version` column exists and how it prevents lost updates.

## What the version column does
- JPA adds a `version` column when an entity has a `@Version` field.
- Every successful update increments `version`.
- If another update tries to save a stale `version`, JPA throws an `OptimisticLockException`.

## Example: Two admins update the same seat
**Initial DB state**
- `seat_id = 101`
- `status = AVAILABLE`
- `version = 3`

**Admin A reads seat 101**
- Receives `version = 3`

**Admin B reads seat 101**
- Receives `version = 3`

**Admin A updates seat (status = DISABLED)**
- Update succeeds
- DB row becomes `version = 4`

**Admin B updates seat (status = AVAILABLE)**
- JPA tries to update with `version = 3`
- DB row is already `version = 4`
- Update fails with `OptimisticLockException`

**Result**
- Admin B does not overwrite Admin A’s change.
- The system can return a `409 Conflict` and ask the client to refresh the seat data.

## Example: Booking seat under concurrent requests
**Initial DB state**
- `seat_id = 201`
- `status = AVAILABLE`
- `version = 7`

**Customer X starts booking**
- Reads seat 201 (`version = 7`)

**Customer Y starts booking (same seat)**
- Reads seat 201 (`version = 7`)

**Customer X completes booking**
- Seat becomes `BOOKED`
- DB row becomes `version = 8`

**Customer Y completes booking**
- Update attempts with `version = 7`
- DB row is `version = 8`
- Update fails with `OptimisticLockException`

**Result**
- Only one booking succeeds.
- The second request can return `409 Conflict` (e.g., `SeatUnavailable`).

## Why this is standard
- It is the default JPA mechanism to prevent lost updates.
- It avoids heavy database locks under normal traffic.
- It is ideal for high-read / moderate-write workloads like ticketing.

## What to do on failure
- Catch the exception and return `409 Conflict`.
- Ask the client to retry after reloading the latest seat state.

