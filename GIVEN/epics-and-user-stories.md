# Event Ticketing System API – Epics & User Stories

> **Product:** Event Ticketing System REST API
> **Tech Stack:** Java 17+, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL/MySQL, JWT
> **Last Updated:** 2026-03-30

---

## EPIC-1: Authentication & Security

| Field | Detail |
|------|--------|
| **Business Value** | Enables user onboarding and secures the entire API surface. No other feature works without this. |
| **Dependencies** | EPIC-7 (Database schema must exist first) |

**Summary:**  
Implement secure user registration, login with JWT token issuance, Bearer token validation on all protected endpoints, and method-level authorization using Spring Security.

**Definition of Done:**
- Users can register and log in via REST endpoints
- JWT tokens are issued with userId, roles, and expiration
- All secured endpoints reject requests without valid tokens
- Method-level @PreAuthorize enforces role/permission checks
- All auth stories pass unit and integration tests

---

### User Stories

#### US-1.1: User Registration

| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-7 (users table) |

**As a** visitor,  
**I want** to register with my name, email, and password,  
**So that** I can create an account and access the system.

**Acceptance Criteria:**
- POST `/api/auth/register` accepts name, email, password
- Email must be unique (409 Conflict if duplicate)
- Password is encrypted using BCrypt before storing
- Returns 201 with user details (excluding password)
- Validation errors return 400 with field-level messages

**Definition of Done:**
- Endpoint implemented, tested, and documented in Swagger
- BCrypt encryption verified in database

---

#### US-1.2: User Login & JWT Generation

| Field | Detail |
|------|--------|
| **Dependencies** | US-1.1 |

**As a** registered user,  
**I want** to log in with my email and password,  
**So that** I receive a JWT token to access secured APIs.

**Acceptance Criteria:**
- POST `/api/auth/login` accepts email and password
- On success, returns JWT token containing userId, roles, and expiration
- On invalid credentials, returns 401 Unauthorized
- Token expiration is configurable via application properties

**Definition of Done:**
- JWT generation and validation logic implemented
- Token payload contains userId, roles, exp claims
- Tested with valid and invalid credentials

---

#### US-1.3: Bearer Token Authentication

| Field | Detail |
|------|--------|
| **Dependencies** | US-1.2 |

**As a** system,  
**I want** to validate the JWT token on every secured request,  
**So that** only authenticated users can access protected endpoints.

**Acceptance Criteria:**
- All secured endpoints require Authorization: Bearer <token> header
- Expired or invalid tokens return 401 Unauthorized
- Missing token returns 401 Unauthorized
- Public endpoints (register, login, health) are accessible without token

**Definition of Done:**
- Spring Security filter chain configured with JWT filter
- Public/private endpoint whitelist verified

---

#### US-1.4: Method-Level Authorization

| Field | Detail |
|------|--------|
| **Dependencies** | US-1.3, EPIC-2 |

**As a** system,  
**I want** to enforce role/permission checks at the method level using @PreAuthorize,  
**So that** users can only invoke APIs they are authorized for.

**Acceptance Criteria:**
- Admin-only endpoints reject CUSTOMER role with 403 Forbidden
- Customer-only endpoints reject ADMIN role where applicable
- @PreAuthorize annotations applied on controller/service methods

**Definition of Done:**
- @EnableMethodSecurity enabled
- Integration tests confirm 403 for unauthorized roles

## EPIC-2: Role & Permission Management

| Field | Detail |
|------|--------|
| **Business Value** | Provides flexible, fine-grained access control. Admins can dynamically control what each role is allowed to do without code changes. |
| **Dependencies** | EPIC-7 (roles, permissions, join tables) |

**Summary:**  
Implement CRUD operations for roles and permissions, and support Many-to-Many assignment of permissions to roles and roles to users. Access control is evaluated dynamically at runtime.

**Definition of Done:**
- Roles and permissions CRUD endpoints functional
- Many-to-Many relationships (User↔Role, Role↔Permission) persist correctly
- Permission changes are reflected in authorization decisions

---

### User Stories

#### US-2.1: Manage Roles (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-7 |

**As an** admin,  
**I want to** create, view, update, and delete roles,  
**So that** I can define access levels for users.

**Acceptance Criteria:**
- CRUD endpoints under /api/admin/roles
- Role has id and name (e.g., ADMIN, CUSTOMER)
- Duplicate role names return 409 Conflict
- Deleting a role in use is handled gracefully (error or cascade)

**Definition of Done:**
- All four CRUD operations tested with happy and error paths

---

#### US-2.2: Manage Permissions (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-7 |

**As an** admin,  
**I want to** create, view, update, and delete permissions,  
**So that** I can define granular access control entries.

**Acceptance Criteria:**
- CRUD endpoints under /api/admin/permissions
- Permission has id and name (e.g., CREATE_EVENT, BOOK_SEAT)
- Duplicate permission names return 409 Conflict

**Definition of Done:**
- All four CRUD operations tested
- Swagger documented

---

#### US-2.3: Assign Permissions to Roles (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-2.1, US-2.2 |

**As an** admin,  
**I want to** assign or revoke permissions for a role,  
**So that** I can control what each role is allowed to do.

**Acceptance Criteria:**
- PUT/PATCH endpoint to update permissions on a role
- Role ↔ Permission is a Many-to-Many relationship
- Changes take effect on next token validation or request

**Definition of Done:**
- Permission assignment persisted in role_permissions join table
- Integration test confirms updated permissions affect access

---

#### US-2.4: Assign Roles to Users (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-2.1 |

**As an** admin,  
**I want to** assign or revoke roles for a user,  
**So that** I can promote or restrict user access.

**Acceptance Criteria:**
- PUT/PATCH endpoint to update roles on a user
- User ↔ Role is a Many-to-Many relationship
- A user can have multiple roles

**Definition of Done:**
- Role assignment persisted in user_roles join table
- User's JWT reflects updated roles on next login

## EPIC-3: Event Management

| Field | Detail |
|------|--------|
| **Business Value** | Events are the core product. Admins need full lifecycle management; customers need to discover and browse events to make bookings. |
| **Dependencies** | EPIC-1, EPIC-2, EPIC-7 |

**Summary:**  
Provide full CRUD for events (admin), including activate/deactivate status control, and a public paginated browse endpoint for customers to discover active events.

**Definition of Done:**
- Admin can create, update, delete, activate, and deactivate events
- Customers can browse and view active events with pagination
- All endpoints secured by role

---

### User Stories

#### US-3.1: Create Event (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-1, EPIC-7 |

**As an** admin,  
**I want to** create a new event with details like name, venue, date, and description,  
**So that** customers can browse and book tickets.

**Acceptance Criteria:**
- POST /api/admin/events with fields: name, description, venue, eventDateTime, status
- Returns 201 with the created event
- Validation errors return 400

**Definition of Done:**
- Event persisted with audit fields (createdAt, updatedAt)
- Swagger documented

---

#### US-3.2: Update Event (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-3.1 |

**As an** admin,  
**I want to** update an existing event's details,  
**So that** event information stays accurate.

**Acceptance Criteria:**
- PUT /api/admin/events/{id} updates all editable fields
- Returns 200 with updated event
- Non-existent event returns 404

**Definition of Done:**
- Update persists correctly
- updatedAt timestamp refreshed

---

#### US-3.3: Delete Event (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-3.1 |

**As an** admin,  
**I want to** delete an event,  
**So that** invalid or cancelled events are removed.

**Acceptance Criteria:**
- DELETE /api/admin/events/{id}
- Returns 204 No Content on success
- Event with existing bookings is handled gracefully (e.g., soft delete or rejection with 409)

**Definition of Done:**
- Deletion tested with and without associated bookings

---

#### US-3.4: Activate / Deactivate Event (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-3.1 |

**As an** admin,  
**I want to** control event status,  
**So that** I can manage visibility and booking.

**Acceptance Criteria:**
- PATCH /api/admin/events/{id}/status with status (ACTIVE/INACTIVE)
- Only ACTIVE events are bookable by customers
- Returns 200 with updated status

**Definition of Done:**
- Status toggle verified
- Inactive events excluded from customer browse

---

#### US-3.5: Browse Events (Customer)

| Field | Detail |
|------|--------|
| **Dependencies** | US-3.1 |

**As a** customer,  
**I want to** view a paginated list of active events,  
**So that** I can choose which event to attend.

**Acceptance Criteria:**
- GET /api/events returns paginated list of ACTIVE events
- Supports page, size, and sort query parameters
- GET /api/events/{id} returns single event details
- Non-existent event returns 404

**Definition of Done:**
- Pagination metadata (totalElements, totalPages, currentPage) included in response

## EPIC-4: Seat Inventory Management

| Field | Detail |
|------|--------|
| **Business Value** | Seats are the bookable inventory. Without seat management, there is nothing for customers to purchase. |
| **Dependencies** | EPIC-3, EPIC-7 |

**Summary:**  
Allow admins to bulk-create seats for events with categories and pricing, manage seat statuses, and provide a view endpoint for customers to see availability.

**Definition of Done:**
- Admin can bulk-create and update seats per event.
- Seat availability is visible to customers.
- Unique constraint (event_id, seat_number) enforced.

---

### User Stories

#### US-4.1: Bulk Create Seats (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-3 (event must exist) |

**As an** admin,  
**I want to** bulk-create seats for an event with category and price,  
**So that** the event has a defined seating inventory.

**Acceptance Criteria:**
- POST /api/admin/events/{eventId}/seats accepts a list of seats.
- Each seat has seatNumber, category, price; default status is AVAILABLE.
- Duplicate (eventId, seatNumber) returns 409 Conflict.
- Returns 201 with created seats.

**Definition of Done:**
- Bulk insert tested with 50+ seats. Unique constraint verified.

---

#### US-4.2: View Seats for Event

| Field | Detail |
|------|--------|
| **Dependencies** | US-4.1 |

**As a** user (admin or customer),  
**I want to** view seats for an event with their availability,  
**So that** I can see which seats are available, booked, or disabled.

**Acceptance Criteria:**
- GET /api/events/{eventId}/seats returns all seats with status.
- Supports filtering by status and category.
- Customers see only ACTIVE event seats.

**Definition of Done:**
- Filtering verified. Customers cannot see seats for INACTIVE events.

---

#### US-4.3: Update Seat Details (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-4.1 |

**As an** admin,  
**I want to** update a seat's category, price, or status,  
**So that** I can manage seating configuration.

**Acceptance Criteria:**
- PUT /api/admin/events/{eventId}/seats/{seatId} updates seat fields.
- Can change status to DISABLED to block a seat from booking.
- Cannot manually set status to BOOKED (only system via booking).

**Definition of Done:**
- Manual BOOKED status rejected with 400. DISABLED seats excluded from booking flow.

## EPIC-5: Booking & Transaction Handling

| Field | Detail |
|------|--------|
| **Business Value** | This is the revenue-generating flow. Customers book seats, the system guarantees atomicity — no double bookings, no orphaned state. |
| **Dependencies** | EPIC-1, EPIC-3, EPIC-4, EPIC-7 |

**Summary:**  
Enable customers to create bookings by selecting available seats, manage booking lifecycle (confirm, cancel, payment failure), and ensure all seat/booking state changes are atomic using @Transactional. Admins can view all bookings.

**Definition of Done:**
- End-to-end booking flow works: create → confirm/cancel/fail.
- All state transitions are transactional — no partial updates on failure.
- Customers see only their own bookings; admins see all.

---

### User Stories

#### US-5.1: Create Booking (Customer)
| Field | Detail |
|------|--------|
| **Dependencies** | EPIC-3, EPIC-4 |

**As a** customer,  
**I want to** select one or more available seats and create a booking,  
**So that** I can reserve my spot at an event.

**Acceptance Criteria:**
- POST /api/bookings accepts eventId and list of seatIds.
- System verifies event is ACTIVE and all seats are AVAILABLE.
- Booking and seat status updates occur in a single @Transactional block.
- If any seat is unavailable, the entire transaction rolls back (no seat status changes).
- Booking is created with status PENDING_PAYMENT.
- Returns 201 with booking details and booking items.

**Definition of Done:**
- Concurrent booking attempts for same seat result in one success and one failure.
- Rollback verified when one seat in a multi-seat booking is unavailable.

---

#### US-5.2: Confirm Booking (Customer)

| Field | Detail |
|------|--------|
| **Dependencies** | US-5.1 |

**As a** customer,  
**I want to** confirm my booking (simulate payment),  
**So that** my seats are officially reserved.

**Acceptance Criteria:**
- PATCH /api/bookings/{id}/confirm changes status from PENDING_PAYMENT to CONFIRMED.
- Only the booking owner can confirm.
- Invalid state transitions return 409 Conflict.

**Definition of Done:**
- State machine validated: only PENDING_PAYMENT → CONFIRMED allowed.

---

#### US-5.3: Cancel Booking (Customer)

| Field | Detail |
|------|--------|
| **Dependencies** | US-5.1 |

**As a** customer,  
**I want to** cancel my booking,  
**So that** the seats become available for others.

**Acceptance Criteria:**
- PATCH /api/bookings/{id}/cancel changes status to CANCELLED.
- Associated seat statuses revert to AVAILABLE in the same transaction.
- Only the booking owner can cancel.
- Already CANCELLED bookings return 409 Conflict.

**Definition of Done:**
- Seats confirmed as AVAILABLE in DB after cancellation. Transactional rollback tested on failure.

---

#### US-5.4: Handle Payment Failure

| Field | Detail |
|------|--------|
| **Dependencies** | US-5.1 |

**As a** system,  
**I want to** mark a booking as PAYMENT_FAILED when payment fails,  
**So that** seats are released and the booking reflects the failure.

**Acceptance Criteria:**
- PATCH /api/bookings/{id}/payment-failed sets status to PAYMENT_FAILED.
- Associated seats revert to AVAILABLE.
- Transition only valid from PENDING_PAYMENT.

**Definition of Done:**
- State transition and seat release verified. Invalid transitions rejected.

---

#### US-5.5: View My Bookings (Customer)

| Field | Detail |
|------|--------|
| **Dependencies** | US-5.1 |

**As a** customer,  
**I want to** view my bookings in a paginated list,  
**So that** I can track my reservations.

**Acceptance Criteria:**
- GET /api/bookings returns paginated bookings for the authenticated user.
- GET /api/bookings/{id} returns booking details with booking items.
- Customers can only see their own bookings (ownership enforced).

**Definition of Done:**
- Pagination metadata included. Customer A cannot see Customer B's bookings.

---

#### US-5.6: View All Bookings (Admin)

| Field | Detail |
|------|--------|
| **Dependencies** | US-5.1 |

**As an** admin,  
**I want to** view all bookings across all users,  
**So that** I can monitor system activity.

**Acceptance Criteria:**
- GET /api/admin/bookings returns paginated list of all bookings.
- Supports filtering by status, eventId, userId.

**Definition of Done:**
- Filters verified.
- Only ADMIN role can access.

## EPIC-6: Exception Handling & Health Check

| Field | Detail |
|------|--------|
| **Business Value** | Consistent error responses improve developer experience and reduce client integration effort. Health check enables monitoring in production. |
| **Dependencies** | None |

**Summary:**  
Implement global exception handling using @ControllerAdvice and provide a public health check endpoint.

**Definition of Done:**
- All exceptions return { timestamp, status, message, path }
- /health returns 200 with UP status without authentication.

---

### User Stories

#### US-6.1: Global Exception Handling

**As a** developer / API consumer,  
**I want** consistent JSON error responses,  
**So that** clients can handle errors reliably.

**Acceptance Criteria:**
- @ControllerAdvice handles all exceptions.
- Handles: 400, 401, 403, 404, 409, 500.

**Definition of Done:**
- All HTTP errors tested.
- No stack traces exposed.

---

#### US-6.2: Health Check Endpoint

**As an** operations team member,  
**I want** a public health endpoint,  
**So that** I can monitor application status.

**Acceptance Criteria:**
- GET /health -> { status: "UP", timestamp: "..." }
- No authentication required.

**Definition of Done:**
- Endpoint accessible without token.

## EPIC-7: Database Design

| Field | Detail |
|------|--------|
| **Business Value** | Foundation for all persistence. Ensures integrity, performance, and efficiency. |
| **Dependencies** | None |

**Summary:**  
Design relational schema using JPA/Hibernate with proper relationships, constraints, foreign keys, and indexes.

**Definition of Done:**
- 9 tables created.
- Constraints and indexes verified.
- ER diagram matches implementation.

---

### User Stories

#### US-7.1: Database Schema Implementation

**Acceptance Criteria:**
- Tables: users, roles, permissions, user_roles, role_permissions, events, seats, bookings, booking_items
- Unique: users.email
- Unique: (event_id, seat_number) in seats
- Foreign keys defined
- Indexes on key columns

**Definition of Done:**
- Hibernate schema matches design.
- ER diagram created.

## EPIC-8: Non-Functional Requirements & Deliverables

| Field | Detail |
|------|--------|
| **Business Value** | Ensures production readiness, maintainability, and documentation. |
| **Dependencies** | All other epics |

**Summary:**  
Add pagination, logging, Swagger docs, and testing.

**Definition of Done:**
- Pagination added
- Logging implemented
- Swagger complete
- Tests added

---

### User Stories

#### US-8.1: Pagination Support

**Acceptance Criteria:**
- Supports page, size, sort
- Response includes totalElements, totalPages, currentPage

---

#### US-8.2: Logging

**Acceptance Criteria:**
- INFO logs for auth & booking
- ERROR logs for exceptions
- No sensitive data logged

---

#### US-8.3: API Documentation (Swagger/OpenAPI)

**Acceptance Criteria:**
- Swagger UI at /swagger-ui.html
- All endpoints documented

**Definition of Done:**
- Try-it-out works with Bearer token

---

#### US-8.4: Unit & Integration Tests

| Field | Detail |
|------|--------|
| **Dependencies** | All service/controller code |

**As a** developer,  
**I want** automated tests for critical flows,  
**So that** regressions are caught early.

**Acceptance Criteria:**
- Unit tests for service layer (auth, booking, event, seat).
- Integration tests for controller endpoints.
- Tests cover happy path and key error scenarios.

**Definition of Done:**
- mvn test passes. Coverage reports generated.

---

# Summary

## Epic Overview

| Epic | Stories | Dependencies |
|------|---------|--------------|
| EPIC-1: Authentication & Security | 4 | EPIC-7 |
| EPIC-2: Role & Permission Management | 4 | EPIC-7 |
| EPIC-3: Event Management | 5 | EPIC-1, EPIC-2, EPIC-7 |
| EPIC-4: Seat Inventory Management | 3 | EPIC-3, EPIC-7 |
| EPIC-5: Booking & Transaction Handling | 6 | EPIC-1, EPIC-3, EPIC-4, EPIC-7 |
| EPIC-6: Exception Handling & Health Check | 2 | None |
| EPIC-7: Database Design | 1 | None |
| EPIC-8: Non-Functional & Deliverables | 4 | All |

| Totals | 29 | |