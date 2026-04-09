# Event Ticketing System — REST API Design

> **Version:** 2.0
> **Last Updated:** 2026-04-04
> **Guidelines:** [Microsoft Azure Architecture Center — Best practices for RESTful web API design](https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design)
> **Tech Stack:** Java 17+, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL/MySQL, JWT

---

## Table of Contents

1. [Design Principles](#1-design-principles)
2. [Base URL & Versioning](#2-base-url--versioning)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Common Headers & Distributed Tracing](#4-common-headers--distributed-tracing)
5. [HTTP Method Semantics & Status Codes](#5-http-method-semantics--status-codes)
6. [Error Response Format](#6-error-response-format)
7. [Pagination & Filtering](#7-pagination--filtering)
8. [HATEOAS — Hypermedia Links](#8-hateoas--hypermedia-links)
9. [Resource Schemas](#9-resource-schemas)
10. [API Endpoint Reference](#10-api-endpoint-reference)
11. [Error Code Reference](#11-error-code-reference)
12. [Appendix A: Booking State Machine](#appendix-a-booking-state-machine)
13. [Appendix B: Richardson Maturity Model](#appendix-b-richardson-maturity-model)
14. [Appendix C: Quick Reference — All Endpoints](#appendix-c-quick-reference--all-endpoints)

---

## 1. Design Principles

The following principles are derived from the [Microsoft Azure Architecture Center — Best practices for RESTful web API design](https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design) and applied to this event ticketing system.

| # | Principle | Application |
|---|-----------|-------------|
| 1 | **Resource-oriented URIs** | URIs are organized around resources using **plural nouns** (e.g., `/events`, `/bookings`). Verbs are never used in URIs — the HTTP method implies the action. |
| 2 | **Standard HTTP methods** | `GET` for reads, `POST` for creates and actions, `PUT` for full replace, `PATCH` for partial update (JSON Merge Patch), `DELETE` for removal. |
| 3 | **Proper status codes** | Each method returns the correct HTTP status code per the guide's prescribed table (see §5). |
| 4 | **Idempotency** | `PUT`, `DELETE`, and `GET` are idempotent. `POST` action endpoints are designed to be safe to retry. |
| 5 | **Stateless requests** | Every request carries all necessary context (JWT token, parameters). No server-side session state. |
| 6 | **Pagination with `limit`/`offset`** | All collection endpoints support `limit` and `offset` query parameters with sensible defaults and a server-enforced `max-limit` to protect against denial-of-service. |
| 7 | **Filtering via query parameters** | Filter collections using intuitive query parameters (e.g., `?status=ACTIVE`, `?category=VIP`). |
| 8 | **HATEOAS** | ~~Responses include a `links` array with navigational hypermedia links (`rel`, `href`, `action`, `types`) to enable client-driven discovery.~~ **Deferred — not implemented in the current version.** |
| 9 | **JSON Merge Patch for PATCH** | Partial updates use `Content-Type: application/merge-patch+json` per [RFC 7396](https://tools.ietf.org/html/rfc7396). A `null` value in the patch body means "remove field." |
| 10 | **URI versioning** | The API uses URL path versioning (`/api/v1`) as the chosen versioning strategy. |
| 11 | **Distributed tracing** | `Correlation-ID` and `X-Request-ID` headers propagate trace context for end-to-end observability. |
| 12 | **Security** | JWT Bearer token required on all non-public endpoints. Role- and permission-based method-level authorization via `@PreAuthorize`. |
| 13 | **Contract-first (OpenAPI)** | API contract is designed and documented first (Swagger/OpenAPI 3.0); implementation follows the contract. |
| 14 | **Avoid mirroring database structure** | URIs model business entities and operations, not database tables. The API is an abstraction layer over the database. |
| 15 | **Date/time format** | RFC 3339 (`YYYY-MM-DDTHH:mm:ssZ`) for all date/time values in request and response bodies. |
| 16 | **camelCase JSON** | All JSON field names use camelCase. |

---

## 2. Base URL & Versioning

```
Base URL:    http://localhost:8080
API Prefix:  /api/v1
```

### Versioning Strategy

This API uses **URI versioning** (`/api/v1`), one of four versioning strategies described in the Microsoft best practices guide:

| Strategy | Example | Adopted? |
|----------|---------|----------|
| **URI versioning** | `/api/v1/events` | **Yes** — chosen for clarity and compatibility with Spring Boot routing conventions |
| Query string versioning | `/api/events?version=1` | No |
| Header versioning | `Custom-Header: api-version=1` | No |
| Media type versioning | `Accept: application/vnd.ticketing.v1+json` | No |

> URI versioning is the simplest to implement, route, and cache. The trade-off acknowledged by the guide is that it complicates HATEOAS links (all links must include the version number), which this API accepts.

**Full URL patterns:**

```
http://localhost:8080/api/v1/{resource-collection}
http://localhost:8080/api/v1/{resource-collection}/{resource-id}
http://localhost:8080/api/v1/{resource-collection}/{resource-id}/{sub-resource}
http://localhost:8080/api/v1/admin/{resource-collection}/{resource-id}
```

**Examples:**

```
GET  http://localhost:8080/api/v1/events
GET  http://localhost:8080/api/v1/events/42
POST http://localhost:8080/api/v1/admin/events
POST http://localhost:8080/api/v1/bookings/7/confirm
GET  http://localhost:8080/api/v1/events/42/seats?status=AVAILABLE&category=VIP
```

> **URI depth limit:** Per the guide's recommendation, URIs do not exceed `collection/item/collection` depth. Deeper hierarchies are avoided in favor of HATEOAS links for related resource navigation.

---

## 3. Authentication & Authorization

### Authentication Mechanism

This API uses **JWT (JSON Web Token)** Bearer token authentication.

- Tokens are obtained by calling `POST /api/v1/auth/login`.
- Every secured request must include the `Authorization` header:
  ```
  Authorization: Bearer <jwt-token>
  ```
- Tokens contain claims: `userId`, `roles`, `exp` (expiration timestamp in Unix epoch seconds).
- Token expiration is configurable via `application.properties`.
- Expired or malformed tokens return `401 Unauthorized`.

### Public Endpoints (no token required)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login and obtain JWT |
| `GET` | `/health` | Health check |

### Authorization Roles

| Role | Access Level |
|------|-------------|
| `ADMIN` | Full access to all admin endpoints and all public/shared endpoints |
| `CUSTOMER` | Access to public event browsing, seat availability, and own bookings only |

### Method-Level Authorization

Spring Security `@EnableMethodSecurity` with `@PreAuthorize` annotations enforce fine-grained permission checks at the method level. Examples:

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAuthority('CREATE_EVENT')")
@PreAuthorize("hasRole('CUSTOMER') and #userId == authentication.principal.id")
```

---

## 4. Common Headers & Distributed Tracing

### Request Headers

| Header | Required | Example | Description |
|--------|----------|---------|-------------|
| `Authorization` | On secured endpoints | `Bearer eyJ0...` | JWT token from login |
| `Content-Type` | On POST, PUT, PATCH | `application/json` or `application/merge-patch+json` | Request body media type |
| `Accept` | Optional | `application/json` | Declares expected response format |
| `Correlation-ID` | Optional | `aaaa0000-bb11-2222-33cc-444444dddddd` | Client-supplied correlation ID for distributed tracing |
| `X-Request-ID` | Optional | `9C4D50EE-2D56-4CD3-8152-34347DC9F2B0` | Caller-supplied unique request ID |

### Response Headers

| Header | When Present | Example | Description |
|--------|-------------|---------|-------------|
| `Content-Type` | Always | `application/json` | Response body media type |
| `Correlation-ID` | When supplied in request | `aaaa0000-bb11-2222-33cc-444444dddddd` | Echoed back; enables end-to-end request tracing |
| `X-Request-ID` | Always | `4227cdc5-9f48-4e84-921a-10967cb785a0` | Service-generated unique request ID for log correlation |
| `Location` | On `201 Created` | `http://localhost:8080/api/v1/events/42` | Absolute URL of the newly created resource |
| `Retry-After` | On `429` or server overload | `60` | Seconds client should wait before retrying |

### Distributed Tracing

Per the Microsoft best practices guide, this API supports distributed tracing through the `Correlation-ID`, `X-Request-ID`, and `X-Trace-ID` headers. These headers propagate trace context as requests flow from client to back-end services:

```
Request:
GET /api/v1/events/42
Correlation-ID: aaaa0000-bb11-2222-33cc-444444dddddd

Response:
HTTP/1.1 200 OK
Correlation-ID: aaaa0000-bb11-2222-33cc-444444dddddd
X-Request-ID: 4227cdc5-9f48-4e84-921a-10967cb785a0
```

> If the client does not supply a `Correlation-ID`, the server generates one. Both the client-supplied ID and the server-generated `X-Request-ID` are logged for traceability.

---

## 5. HTTP Method Semantics & Status Codes

Per the [Microsoft best practices guide](https://learn.microsoft.com/en-us/azure/architecture/best-practices/api-design#define-restful-web-api-methods), the following methods and status codes apply:

### Success Status Codes by Method

| HTTP Method | Operation | Success Codes | Notes |
|-------------|-----------|--------------|-------|
| `GET` (list) | Retrieve a collection | `200 OK` | Returns paginated response with items array |
| `GET` (single) | Retrieve a single resource | `200 OK` | Returns resource representation |
| `GET` (empty result) | Collection with no matches | `200 OK` | Returns empty items array, **not** `204` |
| `POST` (create) | Create a new resource | `201 Created` | `Location` header points to new resource URL |
| `POST` (action) | Invoke an action on a resource | `200 OK` | Response body contains updated resource state |
| `PUT` | Full replace of a resource | `200 OK` or `201 Created` | Idempotent; replaces all mutable fields |
| `PATCH` | Partial update (JSON Merge Patch) | `200 OK` | `Content-Type: application/merge-patch+json`; `null` value means "remove field" |
| `DELETE` | Remove a resource | `204 No Content` | Returns `204` on success |

### PATCH — JSON Merge Patch (RFC 7396)

Per the guide's recommendation, this API uses JSON Merge Patch for partial updates:

- **Media type:** `application/merge-patch+json`
- **Behavior:** The patch document has the same structure as the target resource. Only fields present in the patch body are updated. A field set to `null` means "delete this field."
- **Omitted fields** remain unchanged.

**Example — update an event's venue and description:**

```http
PATCH /api/v1/admin/events/42
Content-Type: application/merge-patch+json

{
  "venue": "Madison Square Garden, New York",
  "description": null
}
```

This updates `venue` to the new value, removes `description` (sets to null/empty), and leaves `name` and `eventDateTime` unchanged.

### Error Status Codes

| Status Code | Meaning | When to Use |
|-------------|---------|-------------|
| `400 Bad Request` | Malformed request or validation failure | Missing required fields, invalid format, malformed patch document |
| `401 Unauthorized` | Authentication required or token invalid | Missing, expired, or malformed JWT |
| `403 Forbidden` | Authenticated but lacks permission | Wrong role, accessing another user's resource |
| `404 Not Found` | Resource does not exist | Unknown resource ID on GET or DELETE |
| `405 Method Not Allowed` | HTTP method not supported on this URI | e.g., POST to a read-only collection |
| `409 Conflict` | State or uniqueness conflict | Duplicate name, seat already booked, invalid booking state transition |
| `415 Unsupported Media Type` | Patch document format not supported | Sending wrong Content-Type for PATCH |
| `429 Too Many Requests` | Rate limit exceeded | Include `Retry-After` header |
| `500 Internal Server Error` | Unexpected server failure | Unhandled runtime exceptions |

---

## 6. Error Response Format

All error responses use a consistent JSON structure aligned with the PRD's standard error format (`timestamp`, `status`, `message`, `path`), extended with machine-readable `error` code and optional field-level `details` for validation errors.

### Error Schema

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    {
      "field": "email",
      "message": "The 'email' field is required."
    },
    {
      "field": "eventDateTime",
      "message": "Must be in RFC 3339 format (YYYY-MM-DDTHH:mm:ssZ)."
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `timestamp` | RFC 3339 | Yes | Server time when the error occurred |
| `status` | integer | Yes | HTTP status code (e.g., `400`, `404`, `409`) |
| `error` | string | Yes | Machine-readable error code (stable across versions; clients may compare against this) |
| `message` | string | Yes | Human-readable error description |
| `path` | string | Yes | The request URI path that caused the error |
| `details` | array | No | Optional list of field-level validation errors |
| `details[].field` | string | Yes | The field or parameter that caused the error |
| `details[].message` | string | Yes | Explanation of the field-level error |

### Error Response Examples

**400 — Validation Error (multiple field failures)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    {
      "field": "email",
      "message": "The 'email' field is required."
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters."
    }
  ]
}
```

**401 — Unauthorized**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid. Provide a valid Bearer token in the Authorization header.",
  "path": "/api/v1/bookings"
}
```

**403 — Forbidden**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to perform this action. Required role: ADMIN.",
  "path": "/api/v1/admin/events"
}
```

**404 — Resource Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/events/999"
}
```

**409 — Conflict (seat unavailable)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "SeatUnavailable",
  "message": "One or more selected seats are not available. The entire booking has been rolled back.",
  "path": "/api/v1/bookings",
  "details": [
    {
      "field": "seatIds[0]",
      "message": "Seat 'A12' has status 'BOOKED' and cannot be reserved."
    }
  ]
}
```

**409 — Conflict (invalid booking state transition)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot confirm a booking with current status 'CANCELLED'. Only PENDING_PAYMENT bookings can be confirmed.",
  "path": "/api/v1/bookings/7/confirm"
}
```

---

## 7. Pagination & Filtering

### Pagination

Per the Microsoft best practices guide, all collection endpoints support pagination via `limit` and `offset` query parameters. The server enforces a `max-limit` to protect against denial-of-service attacks.

#### Query Parameters

| Parameter | Type | Default | Constraints | Description |
|-----------|------|---------|-------------|-------------|
| `limit` | integer | `25` | ≥ 1, ≤ `max-limit` | Maximum number of items to return |
| `offset` | integer | `0` | ≥ 0 | Starting index (zero-based) for the result set |
| `sort` | string | varies by endpoint | — | Sort expression, e.g., `eventDateTime,desc` or `name,asc` |

#### Server-Enforced Limits

| Setting | Value | Description |
|---------|-------|-------------|
| Default `limit` | `25` | Applied when `limit` is not specified |
| `max-limit` | `100` | Server caps `limit` to this value; requests exceeding it are clamped to `100` |
| Default `offset` | `0` | Applied when `offset` is not specified |

> **Tip from the guide:** To prevent denial-of-service attacks, impose an upper limit on the number of items returned. If a client requests `limit=1000` and the server's `max-limit=100`, the server returns 100 items (not an error).

#### Paginated Response Schema

```json
{
  "items": [
    { "...": "resource object" },
    { "...": "resource object" }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 150,
  "links": [
    {
      "rel": "self",
      "href": "http://localhost:8080/api/v1/events?limit=25&offset=0",
      "action": "GET"
    },
    {
      "rel": "next",
      "href": "http://localhost:8080/api/v1/events?limit=25&offset=25",
      "action": "GET"
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `items` | array | Current page of resources |
| `limit` | integer | Number of items requested per page |
| `offset` | integer | Current starting index |
| `totalCount` | integer | Total count of matching resources in the collection |
| `links` | array | HATEOAS navigation links (`self`, `next`, `prev`) — `next` is **omitted** on the last page; `prev` is **omitted** on the first page |

### Filtering

Per the guide, filtering allows clients to refine datasets by applying conditions through query parameters. Each filterable field is a separate query parameter:

```
GET /api/v1/events?status=ACTIVE&sort=eventDateTime,desc
GET /api/v1/events/42/seats?status=AVAILABLE&category=VIP
GET /api/v1/admin/bookings?status=CONFIRMED&eventId=42&userId=5
```

| Endpoint | Supported Filters |
|----------|------------------|
| `GET /api/v1/events` | `status` (always `ACTIVE` for customers) |
| `GET /api/v1/events/{eventId}/seats` | `status` (`AVAILABLE`, `BOOKED`, `DISABLED`), `category` (`VIP`, `STANDARD`, etc.) — **no pagination; all seats returned** |
| `GET /api/v1/admin/bookings` | `status` (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `PAYMENT_FAILED`), `eventId`, `userId` |

> **Validation:** The API validates all filter values. Unknown filter parameters are ignored. Invalid enum values return `400 Bad Request`.

---

## 8. HATEOAS — Hypermedia Links

Per the Microsoft best practices guide, HATEOAS (Hypertext as the Engine of Application State) enables clients to navigate the API without prior knowledge of the URI schema. Each response includes a `links` array with navigational information.

### Link Schema

Each link object in the `links` array has the following structure:

```json
{
  "rel": "string — relationship to the current resource",
  "href": "string — absolute URL of the related resource or action",
  "action": "string — HTTP method to use (GET, POST, PUT, PATCH, DELETE)",
  "types": ["application/json"]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `rel` | string | Relationship name — e.g., `self`, `next`, `prev`, `event`, `seats`, `items`, `confirm`, `cancel` |
| `href` | string | Absolute URL of the target resource or action |
| `action` | string | HTTP method the client should use on the `href` |
| `types` | string[] | Supported media types for the request/response |

### HATEOAS Examples

**Single Event Response:**

```json
{
  "id": 42,
  "name": "Spring Music Festival 2026",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-01-10T08:00:00Z",
  "updatedAt": "2026-01-10T08:00:00Z",
  "links": [
    {
      "rel": "self",
      "href": "http://localhost:8080/api/v1/events/42",
      "action": "GET",
      "types": ["application/json"]
    },
    {
      "rel": "seats",
      "href": "http://localhost:8080/api/v1/events/42/seats",
      "action": "GET",
      "types": ["application/json"]
    }
  ]
}
```

**Single Booking Response (PENDING_PAYMENT):**

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "PENDING_PAYMENT",
  "totalAmount": 349.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [ "..." ],
  "links": [
    {
      "rel": "self",
      "href": "http://localhost:8080/api/v1/bookings/7",
      "action": "GET",
      "types": ["application/json"]
    },
    {
      "rel": "confirm",
      "href": "http://localhost:8080/api/v1/bookings/7/confirm",
      "action": "POST",
      "types": ["application/json"]
    },
    {
      "rel": "cancel",
      "href": "http://localhost:8080/api/v1/bookings/7/cancel",
      "action": "POST",
      "types": ["application/json"]
    },
    {
      "rel": "event",
      "href": "http://localhost:8080/api/v1/events/42",
      "action": "GET",
      "types": ["application/json"]
    }
  ]
}
```

> **State-dependent links:** The set of links changes depending on the state of the resource. A `CONFIRMED` booking would **not** include `confirm` or `cancel` links because those transitions are no longer valid.

---

## 9. Resource Schemas

All JSON field names use **camelCase**. All date/time values use **RFC 3339** format. All resource IDs are **BIGINT** integers (auto-generated by the database).

Fields marked **Read** are system-managed and ignored if provided in request bodies.

---

### 9.1 User

```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "roles": ["CUSTOMER"],
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/admin/users/3", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `name` | string | Create/Update | Required; 2–100 characters |
| `email` | string | Create | Required; valid email format; unique; immutable after creation |
| `roles` | string[] | Read | Managed via `/admin/users/{userId}/roles` |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every modification |

> `password` is **never** returned in any response.

---

### 9.2 Role

```json
{
  "id": 1,
  "name": "CUSTOMER",
  "permissions": ["BOOK_SEAT", "VIEW_EVENT"],
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/admin/roles/1", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `name` | string | Create/Update | Required; unique; e.g., `ADMIN`, `CUSTOMER` |
| `permissions` | string[] | Read | Managed via `/admin/roles/{roleId}/permissions` |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every modification |

---

### 9.3 Permission

```json
{
  "id": 10,
  "name": "CREATE_EVENT",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/admin/permissions/10", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `name` | string | Create/Update | Required; unique; e.g., `CREATE_EVENT`, `BOOK_SEAT`, `DISABLE_SEAT` |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every modification |

---

### 9.4 Event

```json
{
  "id": 42,
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-01-10T08:00:00Z",
  "updatedAt": "2026-01-10T08:00:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/events/42", "action": "GET", "types": ["application/json"] },
    { "rel": "seats", "href": "http://localhost:8080/api/v1/events/42/seats", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `name` | string | Create/Update | Required; max 200 characters |
| `description` | string | Create/Update | Optional; max 1000 characters |
| `venue` | string | Create/Update | Required; max 300 characters |
| `eventDateTime` | RFC 3339 | Create/Update | Required; must be a future date/time |
| `status` | enum string | Read | `ACTIVE` or `INACTIVE`; managed via `PATCH .../status`; defaults to `INACTIVE` on creation |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every modification |

**Status values:** `ACTIVE` | `INACTIVE`

---

### 9.5 Seat

```json
{
  "id": 101,
  "eventId": 42,
  "seatNumber": "A12",
  "category": "VIP",
  "price": 149.99,
  "status": "AVAILABLE",
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T09:00:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/events/42/seats?seatId=101", "action": "GET", "types": ["application/json"] },
    { "rel": "event", "href": "http://localhost:8080/api/v1/events/42", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `eventId` | integer (BIGINT) | Create | Required; must reference an existing event |
| `seatNumber` | string | Create | Required; unique per event (unique constraint on `(event_id, seat_number)`) |
| `category` | string | Create/Update | Required; e.g., `VIP`, `STANDARD`, `ECONOMY` |
| `price` | number (decimal) | Create/Update | Required; must be > 0 |
| `status` | enum string | Update (Admin only) | `AVAILABLE` or `DISABLED` only; `BOOKED` is set exclusively by the booking system |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every modification |

**Status values:** `AVAILABLE` | `BOOKED` | `DISABLED`

> **Important:** Admins cannot manually set `status` to `BOOKED`. Attempting to do so returns `400 Bad Request` with error code `InvalidSeatStatusUpdate`.

---

### 9.6 Booking

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "PENDING_PAYMENT",
  "totalAmount": 349.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    {
      "id": 15,
      "seatId": 101,
      "priceAtBooking": 149.99,
      "createdAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 16,
      "seatId": 102,
      "priceAtBooking": 199.99,
      "createdAt": "2026-04-04T14:30:00Z"
    }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:30:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/bookings/7", "action": "GET", "types": ["application/json"] },
    { "rel": "confirm", "href": "http://localhost:8080/api/v1/bookings/7/confirm", "action": "POST", "types": ["application/json"] },
    { "rel": "cancel", "href": "http://localhost:8080/api/v1/bookings/7/cancel", "action": "POST", "types": ["application/json"] },
    { "rel": "event", "href": "http://localhost:8080/api/v1/events/42", "action": "GET", "types": ["application/json"] }
  ]
}
```

| Field | Type | Mutability | Constraints |
|-------|------|-----------|-------------|
| `id` | integer (BIGINT) | Read | System-assigned, auto-increment |
| `eventId` | integer (BIGINT) | Create | Required; event must be `ACTIVE` |
| `userId` | integer (BIGINT) | Read | Derived from the authenticated user's JWT; not client-supplied |
| `status` | enum string | Read | Managed exclusively via booking action endpoints |
| `totalAmount` | number (decimal) | Read | Sum of all `priceAtBooking` values — snapshotted at booking time |
| `bookingDate` | RFC 3339 | Read | The date/time the booking was initiated; set on insert |
| `items` | BookingItem[] | Create | Required; at least one seat; all seats must be `AVAILABLE` |
| `createdAt` | RFC 3339 | Read | System-assigned on creation |
| `updatedAt` | RFC 3339 | Read | System-updated on every state change |

**Booking status values:** `PENDING_PAYMENT` | `CONFIRMED` | `CANCELLED` | `PAYMENT_FAILED`

**Valid state transitions:**

```
PENDING_PAYMENT  →  CONFIRMED        (via POST /bookings/{id}/confirm)
PENDING_PAYMENT  →  CANCELLED        (via POST /bookings/{id}/cancel)
PENDING_PAYMENT  →  PAYMENT_FAILED   (via POST /bookings/{id}/payment-failed)
```

Any other transition returns `409 Conflict` with error code `InvalidStateTransition`.

> **HATEOAS and state:** The `links` array dynamically reflects available transitions. A `CONFIRMED` booking will **not** include `confirm` or `cancel` links.

---

### 9.7 BookingItem

```json
{
  "id": 15,
  "seatId": 101,
  "priceAtBooking": 149.99,
  "createdAt": "2026-04-04T14:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer (BIGINT) | System-assigned, auto-increment |
| `seatId` | integer (BIGINT) | Reference to the seat that was booked |
| `priceAtBooking` | number (decimal) | **Price snapshot** — the price paid for this seat at booking time; immutable after insert |
| `createdAt` | RFC 3339 | Set on insert |

---

## 10. API Endpoint Reference

---

### Auth Endpoints

#### `POST /api/v1/auth/register`

Register a new user account.

**Authentication:** Public — no token required.

**Request Body:**

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | Yes | 2–100 characters |
| `email` | string | Yes | Valid email format; must be unique |
| `password` | string | Yes | Minimum 8 characters; stored as BCrypt hash |

**Success Response — `201 Created`**

```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "roles": [],
  "createdAt": "2026-04-04T14:00:00Z",
  "updatedAt": "2026-04-04T14:00:00Z",
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/admin/users/3", "action": "GET", "types": ["application/json"] },
    { "rel": "login", "href": "http://localhost:8080/api/v1/auth/login", "action": "POST", "types": ["application/json"] }
  ]
}
```

**Response Headers:**
```
Location: http://localhost:8080/api/v1/admin/users/3
X-Request-ID: 4227cdc5-9f48-4e84-921a-10967cb785a0
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing or invalid fields |
| `409` | `DuplicateEmail` | Email is already registered |

---

#### `POST /api/v1/auth/login`

Authenticate a user and receive a JWT access token.

**Authentication:** Public — no token required.

**Request Body:**

```json
{
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Success Response — `200 OK`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 3,
  "roles": ["CUSTOMER"]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string | JWT token for use in the `Authorization: Bearer` header |
| `tokenType` | string | Always `"Bearer"` |
| `expiresIn` | integer | Token validity in seconds (e.g., `86400` = 24 hours) |
| `userId` | integer | The authenticated user's ID |
| `roles` | string[] | The user's currently assigned roles |

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing email or password |
| `401` | `InvalidCredentials` | Email not found or password incorrect |

---

### User Management (Admin)

#### `GET /api/v1/admin/users`

List all registered users.

**Authentication:** Bearer token — `ADMIN` role required.

**Query Parameters:** `limit` (default: 25), `offset` (default: 0), `sort` (e.g., `name,asc`)

**Success Response — `200 OK`** (paginated)

```json
{
  "items": [
    {
      "id": 3,
      "name": "Jane Doe",
      "email": "jane@example.com",
      "roles": ["CUSTOMER"],
      "createdAt": "2026-01-15T10:30:00Z",
      "updatedAt": "2026-01-15T10:30:00Z",
      "links": [
        { "rel": "self", "href": "http://localhost:8080/api/v1/admin/users/3", "action": "GET", "types": ["application/json"] }
      ]
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 42,
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/admin/users?limit=25&offset=0", "action": "GET" },
    { "rel": "next", "href": "http://localhost:8080/api/v1/admin/users?limit=25&offset=25", "action": "GET" }
  ]
}
```

---

#### `GET /api/v1/admin/users/{userId}`

Get a specific user by their ID.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `userId` — BIGINT ID of the user.

**Success Response — `200 OK`** → User schema (with `links`)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | User with given ID does not exist |

---

#### `PUT /api/v1/admin/users/{userId}/roles`

Replace all roles assigned to a user. Sending an empty array removes all roles.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `userId` — BIGINT ID of the user.

**Request Body:**

```json
{
  "roleIds": [1, 2]
}
```

**Success Response — `200 OK`** → Updated User schema with the new `roles` array.

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | `roleIds` field is missing |
| `404` | `ResourceNotFound` | User or one of the specified roles not found |

---

### Role Management (Admin)

#### `GET /api/v1/admin/roles`

List all roles.

**Authentication:** Bearer token — `ADMIN` role required.

**Query Parameters:** `limit` (default: 25), `offset` (default: 0)

**Success Response — `200 OK`** (paginated) — `items` array of Role schemas.

---

#### `POST /api/v1/admin/roles`

Create a new role.

**Authentication:** Bearer token — `ADMIN` role required.

**Request Body:**

```json
{
  "name": "MODERATOR"
}
```

**Success Response — `201 Created`** → Role schema

**Response Headers:**
```
Location: http://localhost:8080/api/v1/admin/roles/5
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | `name` field missing or blank |
| `409` | `DuplicateRoleName` | A role with this name already exists |

---

#### `GET /api/v1/admin/roles/{roleId}`

Get a role by ID, including its assigned permissions.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `roleId` — BIGINT ID of the role.

**Success Response — `200 OK`** → Role schema (with `links`)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Role with given ID does not exist |

---

#### `PUT /api/v1/admin/roles/{roleId}`

Fully replace a role's name. This is an idempotent operation per the guide — submitting the same request multiple times produces the same result.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `roleId` — BIGINT ID of the role.

**Request Body:**

```json
{
  "name": "SUPER_ADMIN"
}
```

**Success Response — `200 OK`** → Updated Role schema

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Role not found |
| `409` | `DuplicateRoleName` | Another role already has this name |

---

#### `DELETE /api/v1/admin/roles/{roleId}`

Delete a role.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `roleId` — BIGINT ID of the role.

**Success Response — `204 No Content`**

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Role does not exist |
| `409` | `RoleInUse` | Role is currently assigned to one or more users and cannot be deleted |

---

#### `PUT /api/v1/admin/roles/{roleId}/permissions`

Replace all permissions assigned to a role. Sending an empty array removes all permissions.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `roleId` — BIGINT ID of the role.

**Request Body:**

```json
{
  "permissionIds": [10, 11, 12]
}
```

**Success Response — `200 OK`** → Updated Role schema with new `permissions` array.

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Role or one of the specified permissions not found |

---

### Permission Management (Admin)

#### `GET /api/v1/admin/permissions`

List all permissions.

**Authentication:** Bearer token — `ADMIN` role required.

**Query Parameters:** `limit` (default: 25), `offset` (default: 0)

**Success Response — `200 OK`** (paginated) — `items` array of Permission schemas.

---

#### `POST /api/v1/admin/permissions`

Create a new permission.

**Authentication:** Bearer token — `ADMIN` role required.

**Request Body:**

```json
{
  "name": "DISABLE_SEAT"
}
```

**Success Response — `201 Created`** → Permission schema

**Response Headers:**
```
Location: http://localhost:8080/api/v1/admin/permissions/10
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | `name` field missing or blank |
| `409` | `DuplicatePermissionName` | A permission with this name already exists |

---

#### `GET /api/v1/admin/permissions/{permissionId}`

Get a permission by ID.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `permissionId` — BIGINT ID of the permission.

**Success Response — `200 OK`** → Permission schema (with `links`)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Permission with given ID does not exist |

---

#### `PUT /api/v1/admin/permissions/{permissionId}`

Update a permission's name.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `permissionId` — BIGINT ID of the permission.

**Request Body:**

```json
{
  "name": "MANAGE_SEATS"
}
```

**Success Response — `200 OK`** → Updated Permission schema

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Permission not found |
| `409` | `DuplicatePermissionName` | Another permission already has this name |

---

#### `DELETE /api/v1/admin/permissions/{permissionId}`

Delete a permission.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `permissionId` — BIGINT ID of the permission.

**Success Response — `204 No Content`**

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Permission does not exist |
| `409` | `PermissionInUse` | Permission is assigned to one or more roles and cannot be deleted |

---

### Event Management (Admin)

#### `POST /api/v1/admin/events`

Create a new event. The event is created with `status: "INACTIVE"` and must be explicitly activated before customers can browse or book it.

**Authentication:** Bearer token — `ADMIN` role required.

**Request Body:**

```json
{
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | Yes | Max 200 characters |
| `description` | string | No | Max 1000 characters |
| `venue` | string | Yes | Max 300 characters |
| `eventDateTime` | RFC 3339 | Yes | Must be a future date/time |

**Success Response — `201 Created`** → Event schema (with `status: "INACTIVE"` and `links`)

**Response Headers:**
```
Location: http://localhost:8080/api/v1/events/42
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing or invalid fields |

---

#### `PUT /api/v1/admin/events/{eventId}`

Fully replace an event's editable fields. Replaces `name`, `description`, `venue`, and `eventDateTime`. Does not change `status`.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Request Body:**

```json
{
  "name": "Summer Gala 2026",
  "description": "Updated description for the gala.",
  "venue": "Madison Square Garden, New York",
  "eventDateTime": "2026-07-20T20:00:00Z"
}
```

**Success Response — `200 OK`** → Updated Event schema

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing or invalid fields |
| `404` | `ResourceNotFound` | Event not found |

---

#### `DELETE /api/v1/admin/events/{eventId}`

Delete an event. Events with confirmed bookings cannot be hard-deleted.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Success Response — `204 No Content`**

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Event does not exist |
| `409` | `EventHasBookings` | Event has confirmed bookings and cannot be deleted. Deactivate the event instead. |

---

#### `PATCH /api/v1/admin/events/{eventId}/status`

Update an event's status (activate or deactivate).

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Request Body:**

```json
{
  "status": "ACTIVE"
}
```

| Field | Type | Required | Allowed Values |
|-------|------|----------|----------------|
| `status` | string | Yes | `ACTIVE` or `INACTIVE` |

**Success Response — `200 OK`** → Updated Event schema with new status

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Invalid status value |
| `404` | `ResourceNotFound` | Event not found |
| `409` | `EventAlreadyActive` | Event is already `ACTIVE` |
| `409` | `EventAlreadyInactive` | Event is already `INACTIVE` |

---

### Event Browsing (Public/Customer)

#### `GET /api/v1/events`

Retrieve a paginated list of **ACTIVE** events only. Customers cannot see `INACTIVE` events.

**Authentication:** Bearer token — `ADMIN` or `CUSTOMER` role.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `25` | Max items to return (max: 100) |
| `offset` | integer | `0` | Starting index |
| `sort` | string | `eventDateTime,asc` | Sort field and direction |

**Success Response — `200 OK`**

```json
{
  "items": [
    {
      "id": 42,
      "name": "Spring Music Festival 2026",
      "description": "An outdoor music festival featuring top artists.",
      "venue": "Central Park, New York",
      "eventDateTime": "2026-06-15T19:00:00Z",
      "status": "ACTIVE",
      "createdAt": "2026-01-10T08:00:00Z",
      "updatedAt": "2026-01-10T08:00:00Z",
      "links": [
        { "rel": "self", "href": "http://localhost:8080/api/v1/events/42", "action": "GET", "types": ["application/json"] },
        { "rel": "seats", "href": "http://localhost:8080/api/v1/events/42/seats", "action": "GET", "types": ["application/json"] }
      ]
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 50,
  "links": [
    { "rel": "self", "href": "http://localhost:8080/api/v1/events?limit=25&offset=0", "action": "GET" },
    { "rel": "next", "href": "http://localhost:8080/api/v1/events?limit=25&offset=25", "action": "GET" }
  ]
}
```

---

#### `GET /api/v1/events/{eventId}`

Get full details of a single event. Returns `404` for `INACTIVE` events when called by a `CUSTOMER` (the event's existence is not disclosed).

**Authentication:** Bearer token — `ADMIN` or `CUSTOMER` role.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Success Response — `200 OK`** → Event schema (with `links`)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Event not found, or event is INACTIVE (customer context) |

---

### Seat Management (Admin)

#### `POST /api/v1/admin/events/{eventId}/seats`

Bulk-create seats for a specific event. All seats within the request are inserted in a single transaction — if any seat fails the unique constraint, the entire batch is rejected.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Request Body:**

```json
{
  "seats": [
    { "seatNumber": "A1", "category": "VIP", "price": 199.99 },
    { "seatNumber": "A2", "category": "VIP", "price": 199.99 },
    { "seatNumber": "B1", "category": "STANDARD", "price": 79.99 },
    { "seatNumber": "B2", "category": "STANDARD", "price": 79.99 }
  ]
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `seats` | array | Yes | At least 1 item |
| `seats[].seatNumber` | string | Yes | Must be unique per event |
| `seats[].category` | string | Yes | e.g., `VIP`, `STANDARD`, `ECONOMY` |
| `seats[].price` | number | Yes | Must be > 0 |

**Success Response — `201 Created`**

```json
{
  "items": [
    {
      "id": 101,
      "eventId": 42,
      "seatNumber": "A1",
      "category": "VIP",
      "price": 199.99,
      "status": "AVAILABLE",
      "createdAt": "2026-01-10T09:00:00Z",
      "updatedAt": "2026-01-10T09:00:00Z"
    }
  ],
  "created": 4
}
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing or invalid seat fields |
| `404` | `ResourceNotFound` | Event not found |
| `409` | `DuplicateSeatNumber` | One or more seat numbers already exist for this event |

---

#### `PUT /api/v1/admin/events/{eventId}/seats/{seatId}`

Update a single seat's category, price, or status. Cannot be used to set `status` to `BOOKED`.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:**
- `eventId` — BIGINT ID of the event.
- `seatId` — BIGINT ID of the seat.

**Request Body:**

```json
{
  "category": "STANDARD",
  "price": 89.99,
  "status": "DISABLED"
}
```

| Field | Updatable | Allowed Values |
|-------|-----------|----------------|
| `category` | Yes | Any string |
| `price` | Yes | Must be > 0 |
| `status` | Yes (Admin) | `AVAILABLE` or `DISABLED` only — not `BOOKED` |

**Success Response — `200 OK`** → Updated Seat schema

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `InvalidSeatStatusUpdate` | Attempted to set `status` to `BOOKED` |
| `400` | `ValidationFailed` | Invalid field values |
| `404` | `ResourceNotFound` | Seat or event not found |

---

### Seat Browsing (Shared)

#### `GET /api/v1/events/{eventId}/seats`

List all seats for a specific event with their current status and availability.

**Authentication:** Bearer token — `ADMIN` or `CUSTOMER` role.

> Customers can only view seats for **ACTIVE** events.

**Path Parameters:** `eventId` — BIGINT ID of the event.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `status` | string | — | Filter by seat status: `AVAILABLE`, `BOOKED`, or `DISABLED` |
| `category` | string | — | Filter by category: e.g., `VIP`, `STANDARD` |

> **No pagination** — seats for an event are fixed in number. All seats matching the filters are returned in one response.

**Success Response — `200 OK`** (paginated, `items` array of Seat schemas with `links`)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `403` | `EventNotActive` | Customer attempting to view seats for an INACTIVE event |
| `404` | `ResourceNotFound` | Event not found |

---

### Booking Management (Customer)

#### `POST /api/v1/bookings`

Create a new booking by selecting one or more available seats for an active event.

**Authentication:** Bearer token — `CUSTOMER` role required.

> All seat status updates (from `AVAILABLE` to `BOOKED`) and the booking record creation occur within a single `@Transactional` block. If any seat is unavailable, the entire transaction rolls back — no seat statuses are changed.

**Request Body:**

```json
{
  "eventId": 42,
  "seatIds": [101, 102]
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `eventId` | integer | Yes | Event must be `ACTIVE` |
| `seatIds` | integer[] | Yes | At least 1 seat; each seat must be `AVAILABLE`; all must belong to `eventId` |

**Success Response — `201 Created`** → Booking schema with `status: "PENDING_PAYMENT"` and `links` (including `confirm` and `cancel`)

**Response Headers:**
```
Location: http://localhost:8080/api/v1/bookings/7
```

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `400` | `ValidationFailed` | Missing or invalid fields; `seatIds` is empty |
| `404` | `ResourceNotFound` | Event or one of the seats not found |
| `409` | `EventNotActive` | The specified event is not `ACTIVE` |
| `409` | `SeatUnavailable` | One or more seats are `BOOKED` or `DISABLED`; entire request rolled back |

---

#### `GET /api/v1/bookings`

List the authenticated customer's own bookings in descending order of creation.

**Authentication:** Bearer token — `CUSTOMER` role required.

**Query Parameters:** `limit` (default: 25), `offset` (default: 0), `sort` (e.g., `createdAt,desc`)

**Success Response — `200 OK`** (paginated, `items` array of Booking schemas — only the authenticated user's bookings)

---

#### `GET /api/v1/bookings/{bookingId}`

Get full details of a specific booking, including all booking items.

**Authentication:** Bearer token — `CUSTOMER` (owner only) or `ADMIN`.

**Path Parameters:** `bookingId` — BIGINT ID of the booking.

**Success Response — `200 OK`** → Booking schema with populated `items` array and state-specific `links`.

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `403` | `Forbidden` | Authenticated customer does not own this booking |
| `404` | `ResourceNotFound` | Booking not found |

---

#### `POST /api/v1/bookings/{bookingId}/confirm`

Confirm a booking, simulating a successful payment. Transitions status from `PENDING_PAYMENT` to `CONFIRMED`.

**Authentication:** Bearer token — `CUSTOMER` role required (booking owner only).

**Path Parameters:** `bookingId` — BIGINT ID of the booking.

**Request Body:** Empty body or `{}`

**Success Response — `200 OK`** → Booking schema with `status: "CONFIRMED"` (no `confirm`/`cancel` links)

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `403` | `Forbidden` | Not the booking owner |
| `404` | `ResourceNotFound` | Booking not found |
| `409` | `InvalidStateTransition` | Booking is not in `PENDING_PAYMENT` status |

---

#### `POST /api/v1/bookings/{bookingId}/cancel`

Cancel a booking. Transitions status to `CANCELLED` and atomically releases all associated seats back to `AVAILABLE`.

**Authentication:** Bearer token — `CUSTOMER` role required (booking owner only).

**Path Parameters:** `bookingId` — BIGINT ID of the booking.

**Request Body:** Empty body or `{}`

> Seat status reverts to `AVAILABLE` in the same `@Transactional` block as the status change.

**Success Response — `200 OK`** → Booking schema with `status: "CANCELLED"`

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `403` | `Forbidden` | Not the booking owner |
| `404` | `ResourceNotFound` | Booking not found |
| `409` | `InvalidStateTransition` | Booking is already `CANCELLED`, `CONFIRMED`, or `PAYMENT_FAILED` |

---

#### `POST /api/v1/bookings/{bookingId}/payment-failed`

Report a payment failure for a booking. Transitions status from `PENDING_PAYMENT` to `PAYMENT_FAILED` and atomically releases all seats back to `AVAILABLE`.

**Authentication:** Bearer token — `CUSTOMER` role or internal system call.

**Path Parameters:** `bookingId` — BIGINT ID of the booking.

**Request Body:** Empty body or `{}`

> Seat status reverts to `AVAILABLE` in the same `@Transactional` block as the status change.

**Success Response — `200 OK`** → Booking schema with `status: "PAYMENT_FAILED"`

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Booking not found |
| `409` | `InvalidStateTransition` | Booking is not in `PENDING_PAYMENT` status |

---

### Booking Management (Admin)

#### `GET /api/v1/admin/bookings`

List all bookings across all users, with optional filtering.

**Authentication:** Bearer token — `ADMIN` role required.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `25` | Max items to return (max: 100) |
| `offset` | integer | `0` | Starting index |
| `sort` | string | `createdAt,desc` | Sort direction |
| `status` | string | — | Filter by booking status: `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `PAYMENT_FAILED` |
| `eventId` | integer | — | Filter by event ID |
| `userId` | integer | — | Filter by user ID |

**Success Response — `200 OK`** (paginated, `items` array of Booking schemas with `links`)

---

#### `GET /api/v1/admin/bookings/{bookingId}`

Get a specific booking by ID — admin view with no ownership restriction.

**Authentication:** Bearer token — `ADMIN` role required.

**Path Parameters:** `bookingId` — BIGINT ID of the booking.

**Success Response — `200 OK`** → Booking schema with populated `items` array and `links`.

**Error Responses:**

| Status | `error` Code | Description |
|--------|-------------|-------------|
| `404` | `ResourceNotFound` | Booking not found |

---

### Health Check

#### `GET /health`

Returns the current health status of the application. No authentication required.

**Authentication:** Public — no token required.

**Success Response — `200 OK`**

```json
{
  "status": "UP",
  "timestamp": "2026-04-04T14:00:00Z"
}
```

---

## 11. Error Code Reference

The following error codes are part of the API contract. They are returned in the `error` field of the JSON error response body. These codes **must not change** between API versions as client code may compare against them.

| Error Code | HTTP Status | Description |
|-----------|------------|-------------|
| `ValidationFailed` | 400 | One or more request fields failed validation |
| `InvalidSeatStatusUpdate` | 400 | Admin attempted to manually set a seat's status to `BOOKED` |
| `Unauthorized` | 401 | JWT token is missing, expired, or malformed |
| `InvalidCredentials` | 401 | The provided email or password is incorrect |
| `Forbidden` | 403 | Authenticated user lacks the required role, permission, or resource ownership |
| `EventNotActive` | 403/409 | Customer attempting to access or book an `INACTIVE` event |
| `ResourceNotFound` | 404 | The requested resource does not exist |
| `DuplicateEmail` | 409 | A user with this email address is already registered |
| `DuplicateRoleName` | 409 | A role with this name already exists |
| `DuplicatePermissionName` | 409 | A permission with this name already exists |
| `DuplicateSeatNumber` | 409 | A seat with this seat number already exists for the given event |
| `RoleInUse` | 409 | Role cannot be deleted because it is assigned to one or more users |
| `PermissionInUse` | 409 | Permission cannot be deleted because it is assigned to one or more roles |
| `EventHasBookings` | 409 | Event cannot be hard-deleted due to associated bookings; deactivate instead |
| `EventAlreadyActive` | 409 | Event is already in `ACTIVE` status |
| `EventAlreadyInactive` | 409 | Event is already in `INACTIVE` status |
| `SeatUnavailable` | 409 | One or more seats are not in `AVAILABLE` status; booking rolled back |
| `InvalidStateTransition` | 409 | The requested booking status transition is not permitted |
| `UnsupportedMediaType` | 415 | Patch document format not supported (expected `application/merge-patch+json`) |
| `TooManyRequests` | 429 | Rate limit exceeded; retry after `Retry-After` seconds |
| `InternalServerError` | 500 | An unexpected server error occurred |

---

## Appendix A: Booking State Machine

```
                    ┌──────────────────┐
                    │  PENDING_PAYMENT  │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
          /confirm       /cancel     /payment-failed
              │              │              │
              ▼              ▼              ▼
        ┌──────────┐  ┌───────────┐  ┌───────────────┐
        │CONFIRMED │  │ CANCELLED │  │ PAYMENT_FAILED │
        └──────────┘  └───────────┘  └───────────────┘
```

| From Status | Action Endpoint | To Status | Seat Effect |
|------------|----------------|-----------|-------------|
| `PENDING_PAYMENT` | `POST .../confirm` | `CONFIRMED` | No change (seats remain `BOOKED`) |
| `PENDING_PAYMENT` | `POST .../cancel` | `CANCELLED` | All seats revert to `AVAILABLE` (same transaction) |
| `PENDING_PAYMENT` | `POST .../payment-failed` | `PAYMENT_FAILED` | All seats revert to `AVAILABLE` (same transaction) |
| Any other transition | — | — | Returns `409 InvalidStateTransition` |

---

## Appendix B: Richardson Maturity Model

Per the Microsoft best practices guide, the [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html) defines four levels of REST maturity:

| Level | Description | This API |
|-------|-------------|----------|
| **Level 0** | Single URI, all POST (e.g., SOAP) | — |
| **Level 1** | Separate URIs for individual resources | ✅ |
| **Level 2** | Use HTTP methods (GET, POST, PUT, PATCH, DELETE) with correct status codes | ✅ |
| **Level 3** | HATEOAS — hypermedia links in responses for state-driven navigation | ⏳ Deferred |

> **Current maturity: Level 2.** HATEOAS `links` arrays are deferred to a future version.

---

## Appendix C: Quick Reference — All Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/auth/register` | Public | Register new user |
| `POST` | `/api/v1/auth/login` | Public | Login and obtain JWT |
| `GET` | `/api/v1/admin/users` | ADMIN | List all users |
| `GET` | `/api/v1/admin/users/{userId}` | ADMIN | Get user by ID |
| `PUT` | `/api/v1/admin/users/{userId}/roles` | ADMIN | Assign roles to user |
| `GET` | `/api/v1/admin/roles` | ADMIN | List all roles |
| `POST` | `/api/v1/admin/roles` | ADMIN | Create a role |
| `GET` | `/api/v1/admin/roles/{roleId}` | ADMIN | Get role by ID |
| `PUT` | `/api/v1/admin/roles/{roleId}` | ADMIN | Update role name |
| `DELETE` | `/api/v1/admin/roles/{roleId}` | ADMIN | Delete a role |
| `PUT` | `/api/v1/admin/roles/{roleId}/permissions` | ADMIN | Assign permissions to role |
| `GET` | `/api/v1/admin/permissions` | ADMIN | List all permissions |
| `POST` | `/api/v1/admin/permissions` | ADMIN | Create a permission |
| `GET` | `/api/v1/admin/permissions/{permissionId}` | ADMIN | Get permission by ID |
| `PUT` | `/api/v1/admin/permissions/{permissionId}` | ADMIN | Update permission name |
| `DELETE` | `/api/v1/admin/permissions/{permissionId}` | ADMIN | Delete a permission |
| `POST` | `/api/v1/admin/events` | ADMIN | Create an event |
| `PUT` | `/api/v1/admin/events/{eventId}` | ADMIN | Update an event |
| `DELETE` | `/api/v1/admin/events/{eventId}` | ADMIN | Delete an event |
| `PATCH` | `/api/v1/admin/events/{eventId}/status` | ADMIN | Activate / deactivate event |
| `POST` | `/api/v1/admin/events/{eventId}/seats` | ADMIN | Bulk create seats |
| `PUT` | `/api/v1/admin/events/{eventId}/seats/{seatId}` | ADMIN | Update a seat |
| `GET` | `/api/v1/admin/bookings` | ADMIN | List all bookings |
| `GET` | `/api/v1/admin/bookings/{bookingId}` | ADMIN | Get booking by ID |
| `GET` | `/api/v1/events` | CUSTOMER / ADMIN | Browse active events (paginated) |
| `GET` | `/api/v1/events/{eventId}` | CUSTOMER / ADMIN | Get event details |
| `GET` | `/api/v1/events/{eventId}/seats` | CUSTOMER / ADMIN | View seats for an event |
| `POST` | `/api/v1/bookings` | CUSTOMER | Create a booking |
| `GET` | `/api/v1/bookings` | CUSTOMER | View my bookings |
| `GET` | `/api/v1/bookings/{bookingId}` | CUSTOMER / ADMIN | Get booking details |
| `POST` | `/api/v1/bookings/{bookingId}/confirm` | CUSTOMER | Confirm booking (simulate payment) |
| `POST` | `/api/v1/bookings/{bookingId}/cancel` | CUSTOMER | Cancel booking |
| `POST` | `/api/v1/bookings/{bookingId}/payment-failed` | CUSTOMER | Report payment failure |
| `GET` | `/health` | Public | Health check |

---

*Total endpoints: **34***
*Total resources: **7** (User, Role, Permission, Event, Seat, Booking, BookingItem)*
*Current maturity: **Richardson Level 2** (HATEOAS deferred)*
*Pagination model: **`limit`/`offset`** with server-enforced `max-limit` (seats endpoint: no pagination)*
*Patch format: **JSON Merge Patch (RFC 7396)***
*API contract: **OpenAPI 3.0 (Swagger)***
