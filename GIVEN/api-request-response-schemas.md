# Event Ticketing System — API Request & Response JSON Schemas

> **Version:** 2.1
> **Last Updated:** 2026-04-08
> **Changes in v2.1:** HATEOAS links deferred (not implemented). Seat browsing changed from paginated to flat list response.
> **Source:** [rest-api-design.md](rest-api-design.md)
> **Convention:** All IDs are BIGINT integers · All dates are RFC 3339 · All JSON fields are camelCase

---

## Table of Contents

1. [Common Schemas](#1-common-schemas)
2. [Auth Endpoints](#2-auth-endpoints)
3. [User Management (Admin)](#3-user-management-admin)
4. [Role Management (Admin)](#4-role-management-admin)
5. [Permission Management (Admin)](#5-permission-management-admin)
6. [Event Management (Admin)](#6-event-management-admin)
7. [Event Browsing (Public/Customer)](#7-event-browsing-publiccustomer)
8. [Seat Management (Admin)](#8-seat-management-admin)
9. [Seat Browsing (Shared)](#9-seat-browsing-shared)
10. [Booking Management (Customer)](#10-booking-management-customer)
11. [Booking Management (Admin)](#11-booking-management-admin)
12. [Health Check](#12-health-check)

---

## 1. Common Schemas

### Error Response Schema

All error responses across every endpoint use this structure:

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ErrorCode",
  "message": "Human-readable description.",
  "path": "/api/v1/resource",
  "details": [
    {
      "field": "fieldName",
      "message": "Field-level error description."
    }
  ]
}
```

> `details` is **optional** — present only for validation errors with multiple field failures.

---

### Paginated Response Schema

All collection `GET` endpoints return this wrapper:

```json
{
  "items": [],
  "limit": 25,
  "offset": 0,
  "totalCount": 0
}
```

> `next` / `prev` navigation links are **not implemented**.

---

### Common Failure Responses (applicable to all secured endpoints)

#### 401 — Unauthorized (missing token)

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid. Provide a valid Bearer token in the Authorization header.",
  "path": "/api/v1/{endpoint}"
}
```

#### 401 — Unauthorized (expired token)

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token has expired. Please login again to obtain a new token.",
  "path": "/api/v1/{endpoint}"
}
```

#### 403 — Forbidden (insufficient role)

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to perform this action. Required role: ADMIN.",
  "path": "/api/v1/admin/{endpoint}"
}
```

#### 500 — Internal Server Error

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 500,
  "error": "InternalServerError",
  "message": "An unexpected server error occurred. Please try again later.",
  "path": "/api/v1/{endpoint}"
}
```

---

## 2. Auth Endpoints

---

### 2.1 `POST /api/v1/auth/register`

**Auth:** Public

#### Request

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

#### Success Response — `201 Created`

*(No response body)*

#### Failure Responses

**400 — Validation Failed (missing fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "name", "message": "The 'name' field is required." },
    { "field": "email", "message": "The 'email' field is required." },
    { "field": "password", "message": "The 'password' field is required." }
  ]
}
```

**400 — Validation Failed (invalid email format)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "email", "message": "Must be a valid email address." }
  ]
}
```

**400 — Validation Failed (password too short)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "password", "message": "Password must be at least 8 characters." }
  ]
}
```

**400 — Validation Failed (name too short)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "name", "message": "Name must be between 2 and 100 characters." }
  ]
}
```

**409 — Duplicate Email**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicateEmail",
  "message": "A user with email 'jane@example.com' is already registered.",
  "path": "/api/v1/auth/register"
}
```

---

### 2.2 `POST /api/v1/auth/login`

**Auth:** Public

#### Request

```json
{
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

#### Success Response — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjMsInJvbGVzIjpbIkNVU1RPTUVSIl0sImV4cCI6MTc3ODAwMTYwMH0.signature",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 3,
  "roles": ["CUSTOMER"]
}
```

#### Failure Responses

**400 — Validation Failed (missing fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/auth/login",
  "details": [
    { "field": "email", "message": "The 'email' field is required." },
    { "field": "password", "message": "The 'password' field is required." }
  ]
}
```

**401 — Invalid Credentials (wrong email)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 401,
  "error": "InvalidCredentials",
  "message": "Invalid email or password.",
  "path": "/api/v1/auth/login"
}
```

**401 — Invalid Credentials (wrong password)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 401,
  "error": "InvalidCredentials",
  "message": "Invalid email or password.",
  "path": "/api/v1/auth/login"
}
```

---

## 3. User Management (Admin)

---

### 3.1 `GET /api/v1/admin/users`

**Auth:** ADMIN

#### Request

No request body. Query parameters: `limit`, `offset`, `sort`.

```
GET /api/v1/admin/users?limit=25&offset=0&sort=name,asc
```

#### Success Response — `200 OK`

```json
{
  "items": [
    {
      "id": 3,
      "name": "Jane Doe",
      "email": "jane@example.com",
      "roles": ["CUSTOMER"]
    },
    {
      "id": 1,
      "name": "Admin User",
      "email": "admin@example.com",
      "roles": ["ADMIN"]
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 2
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 3.2 `GET /api/v1/admin/users/{userId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `userId`.

```
GET /api/v1/admin/users/3
```

#### Success Response — `200 OK`

```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "roles": ["CUSTOMER"]
}
```

#### Failure Responses

**404 — User Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The user with id '999' was not found.",
  "path": "/api/v1/admin/users/999"
}
```

---

### 3.3 `PUT /api/v1/admin/users/{userId}/roles`

**Auth:** ADMIN

#### Request

```json
{
  "roleIds": [1, 2]
}
```

#### Success Response — `200 OK`

```json
{
  "id": 3,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "roles": ["CUSTOMER", "ADMIN"]
}
```

#### Failure Responses

**400 — Validation Failed (missing roleIds)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/users/3/roles",
  "details": [
    { "field": "roleIds", "message": "The 'roleIds' field is required." }
  ]
}
```

**404 — User Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The user with id '999' was not found.",
  "path": "/api/v1/admin/users/999/roles"
}
```

**404 — Role Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The role with id '999' was not found.",
  "path": "/api/v1/admin/users/3/roles"
}
```

---

## 4. Role Management (Admin)

---

### 4.1 `GET /api/v1/admin/roles`

**Auth:** ADMIN

#### Request

No request body. Query parameters: `limit`, `offset`.

```
GET /api/v1/admin/roles?limit=25&offset=0
```

#### Success Response — `200 OK`

```json
{
  "items": [
    {
      "id": 1,
      "name": "ADMIN",
      "permissions": ["CREATE_EVENT", "MANAGE_SEATS", "VIEW_BOOKINGS"]
    },
    {
      "id": 2,
      "name": "CUSTOMER",
      "permissions": ["BOOK_SEAT", "VIEW_EVENT"]
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 2
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 4.2 `POST /api/v1/admin/roles`

**Auth:** ADMIN

#### Request

```json
{
  "name": "MODERATOR"
}
```

#### Success Response — `201 Created`

```json
{
  "id": 5,
  "name": "MODERATOR",
  "permissions": []
}
```

#### Failure Responses

**400 — Validation Failed (name missing)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/roles",
  "details": [
    { "field": "name", "message": "The 'name' field is required." }
  ]
}
```

**400 — Validation Failed (name blank)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/roles",
  "details": [
    { "field": "name", "message": "The 'name' field must not be blank." }
  ]
}
```

**409 — Duplicate Role Name**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicateRoleName",
  "message": "A role with name 'MODERATOR' already exists.",
  "path": "/api/v1/admin/roles"
}
```

---

### 4.3 `GET /api/v1/admin/roles/{roleId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `roleId`.

```
GET /api/v1/admin/roles/1
```

#### Success Response — `200 OK`

```json
{
  "id": 1,
  "name": "ADMIN",
  "permissions": ["CREATE_EVENT", "MANAGE_SEATS", "VIEW_BOOKINGS"]
}
```

#### Failure Responses

**404 — Role Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The role with id '999' was not found.",
  "path": "/api/v1/admin/roles/999"
}
```

---

### 4.4 `PUT /api/v1/admin/roles/{roleId}`

**Auth:** ADMIN

#### Request

```json
{
  "name": "SUPER_ADMIN"
}
```

#### Success Response — `200 OK`

```json
{
  "id": 1,
  "name": "SUPER_ADMIN",
  "permissions": ["CREATE_EVENT", "MANAGE_SEATS", "VIEW_BOOKINGS"]
}
```

#### Failure Responses

**400 — Validation Failed (name missing)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/roles/1",
  "details": [
    { "field": "name", "message": "The 'name' field is required." }
  ]
}
```

**404 — Role Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The role with id '999' was not found.",
  "path": "/api/v1/admin/roles/999"
}
```

**409 — Duplicate Role Name**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicateRoleName",
  "message": "A role with name 'CUSTOMER' already exists.",
  "path": "/api/v1/admin/roles/1"
}
```

---

### 4.5 `DELETE /api/v1/admin/roles/{roleId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `roleId`.

```
DELETE /api/v1/admin/roles/5
```

#### Success Response — `204 No Content`

*(No response body)*

#### Failure Responses

**404 — Role Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The role with id '999' was not found.",
  "path": "/api/v1/admin/roles/999"
}
```

**409 — Role In Use**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "RoleInUse",
  "message": "Role 'CUSTOMER' cannot be deleted because it is assigned to one or more users.",
  "path": "/api/v1/admin/roles/2"
}
```

---

### 4.6 `PUT /api/v1/admin/roles/{roleId}/permissions`

**Auth:** ADMIN

#### Request

```json
{
  "permissionIds": [10, 11, 12]
}
```

#### Success Response — `200 OK`

```json
{
  "id": 2,
  "name": "CUSTOMER",
  "permissions": ["BOOK_SEAT", "VIEW_EVENT", "CANCEL_BOOKING"],
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Failure Responses

**404 — Role Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The role with id '999' was not found.",
  "path": "/api/v1/admin/roles/999/permissions"
}
```

**404 — Permission Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The permission with id '999' was not found.",
  "path": "/api/v1/admin/roles/2/permissions"
}
```

---

## 5. Permission Management (Admin)

---

### 5.1 `GET /api/v1/admin/permissions`

**Auth:** ADMIN

#### Request

No request body. Query parameters: `limit`, `offset`.

```
GET /api/v1/admin/permissions?limit=25&offset=0
```

#### Success Response — `200 OK`

```json
{
  "items": [
    {
      "id": 10,
      "name": "CREATE_EVENT",
      "createdAt": "2026-01-01T00:00:00Z",
      "updatedAt": "2026-01-01T00:00:00Z"
    },
    {
      "id": 11,
      "name": "BOOK_SEAT",
      "createdAt": "2026-01-01T00:00:00Z",
      "updatedAt": "2026-01-01T00:00:00Z"
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 2
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 5.2 `POST /api/v1/admin/permissions`

**Auth:** ADMIN

#### Request

```json
{
  "name": "DISABLE_SEAT"
}
```

#### Success Response — `201 Created`

```json
{
  "id": 15,
  "name": "DISABLE_SEAT",
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (name missing)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/permissions",
  "details": [
    { "field": "name", "message": "The 'name' field is required." }
  ]
}
```

**400 — Validation Failed (name blank)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/permissions",
  "details": [
    { "field": "name", "message": "The 'name' field must not be blank." }
  ]
}
```

**409 — Duplicate Permission Name**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicatePermissionName",
  "message": "A permission with name 'DISABLE_SEAT' already exists.",
  "path": "/api/v1/admin/permissions"
}
```

---

### 5.3 `GET /api/v1/admin/permissions/{permissionId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `permissionId`.

```
GET /api/v1/admin/permissions/10
```

#### Success Response — `200 OK`

```json
{
  "id": 10,
  "name": "CREATE_EVENT",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-01-01T00:00:00Z"
}
```

#### Failure Responses

**404 — Permission Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The permission with id '999' was not found.",
  "path": "/api/v1/admin/permissions/999"
}
```

---

### 5.4 `PUT /api/v1/admin/permissions/{permissionId}`

**Auth:** ADMIN

#### Request

```json
{
  "name": "MANAGE_SEATS"
}
```

#### Success Response — `200 OK`

```json
{
  "id": 10,
  "name": "MANAGE_SEATS",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (name missing)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/permissions/10",
  "details": [
    { "field": "name", "message": "The 'name' field is required." }
  ]
}
```

**404 — Permission Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The permission with id '999' was not found.",
  "path": "/api/v1/admin/permissions/999"
}
```

**409 — Duplicate Permission Name**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicatePermissionName",
  "message": "A permission with name 'BOOK_SEAT' already exists.",
  "path": "/api/v1/admin/permissions/10"
}
```

---

### 5.5 `DELETE /api/v1/admin/permissions/{permissionId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `permissionId`.

```
DELETE /api/v1/admin/permissions/15
```

#### Success Response — `204 No Content`

*(No response body)*

#### Failure Responses

**404 — Permission Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The permission with id '999' was not found.",
  "path": "/api/v1/admin/permissions/999"
}
```

**409 — Permission In Use**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "PermissionInUse",
  "message": "Permission 'BOOK_SEAT' cannot be deleted because it is assigned to one or more roles.",
  "path": "/api/v1/admin/permissions/11"
}
```

---

## 6. Event Management (Admin)

---

### 6.1 `POST /api/v1/admin/events`

**Auth:** ADMIN

#### Request

```json
{
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z"
}
```

#### Success Response — `201 Created`

```json
{
  "id": 42,
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z",
  "status": "INACTIVE",
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (missing required fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events",
  "details": [
    { "field": "name", "message": "The 'name' field is required." },
    { "field": "venue", "message": "The 'venue' field is required." },
    { "field": "eventDateTime", "message": "The 'eventDateTime' field is required." }
  ]
}
```

**400 — Validation Failed (past date)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events",
  "details": [
    { "field": "eventDateTime", "message": "Event date/time must be in the future." }
  ]
}
```

**400 — Validation Failed (field length exceeded)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events",
  "details": [
    { "field": "name", "message": "Name must not exceed 200 characters." }
  ]
}
```

---

### 6.2 `PUT /api/v1/admin/events/{eventId}`

**Auth:** ADMIN

#### Request

```json
{
  "name": "Summer Gala 2026",
  "description": "Updated description for the gala.",
  "venue": "Madison Square Garden, New York",
  "eventDateTime": "2026-07-20T20:00:00Z"
}
```

#### Success Response — `200 OK`

```json
{
  "id": 42,
  "name": "Summer Gala 2026",
  "description": "Updated description for the gala.",
  "venue": "Madison Square Garden, New York",
  "eventDateTime": "2026-07-20T20:00:00Z",
  "status": "INACTIVE",
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T15:00:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (missing fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42",
  "details": [
    { "field": "name", "message": "The 'name' field is required." },
    { "field": "venue", "message": "The 'venue' field is required." }
  ]
}
```

**400 — Validation Failed (invalid date format)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42",
  "details": [
    { "field": "eventDateTime", "message": "Must be in RFC 3339 format (YYYY-MM-DDTHH:mm:ssZ)." }
  ]
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/admin/events/999"
}
```

---

### 6.3 `DELETE /api/v1/admin/events/{eventId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `eventId`.

```
DELETE /api/v1/admin/events/42
```

#### Success Response — `204 No Content`

*(No response body)*

#### Failure Responses

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/admin/events/999"
}
```

**409 — Event Has Bookings**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "EventHasBookings",
  "message": "Event 'Spring Music Festival 2026' cannot be deleted because it has confirmed bookings. Deactivate the event instead.",
  "path": "/api/v1/admin/events/42"
}
```

---

### 6.4 `PATCH /api/v1/admin/events/{eventId}/status`

**Auth:** ADMIN

#### Request — Activate

```json
{
  "status": "ACTIVE"
}
```

#### Request — Deactivate

```json
{
  "status": "INACTIVE"
}
```

#### Success Response — `200 OK` (activation)

```json
{
  "id": 42,
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T15:00:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (invalid status value)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42/status",
  "details": [
    { "field": "status", "message": "Status must be 'ACTIVE' or 'INACTIVE'." }
  ]
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/admin/events/999/status"
}
```

**409 — Event Already Active**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "EventAlreadyActive",
  "message": "Event 'Spring Music Festival 2026' is already in ACTIVE status.",
  "path": "/api/v1/admin/events/42/status"
}
```

**409 — Event Already Inactive**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "EventAlreadyInactive",
  "message": "Event 'Spring Music Festival 2026' is already in INACTIVE status.",
  "path": "/api/v1/admin/events/42/status"
}
```

---

## 7. Event Browsing (Public/Customer)

---

### 7.1 `GET /api/v1/events`

**Auth:** CUSTOMER / ADMIN

#### Request

No request body. Query parameters: `limit`, `offset`, `sort`.

```
GET /api/v1/events?limit=25&offset=0&sort=eventDateTime,asc
```

#### Success Response — `200 OK`

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
      "updatedAt": "2026-03-20T12:00:00Z"
    },
    {
      "id": 43,
      "name": "Summer Jazz Night",
      "description": "An evening of smooth jazz.",
      "venue": "Blue Note, NYC",
      "eventDateTime": "2026-07-10T20:00:00Z",
      "status": "ACTIVE",
      "createdAt": "2026-02-01T10:00:00Z",
      "updatedAt": "2026-02-15T08:00:00Z"
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 50
}
```

#### Success Response — `200 OK` (empty — no active events)

```json
{
  "items": [],
  "limit": 25,
  "offset": 0,
  "totalCount": 0
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 7.2 `GET /api/v1/events/{eventId}`

**Auth:** CUSTOMER / ADMIN

#### Request

No request body. Path parameter: `eventId`.

```
GET /api/v1/events/42
```

#### Success Response — `200 OK`

```json
{
  "id": 42,
  "name": "Spring Music Festival 2026",
  "description": "An outdoor music festival featuring top artists.",
  "venue": "Central Park, New York",
  "eventDateTime": "2026-06-15T19:00:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-01-10T08:00:00Z",
  "updatedAt": "2026-03-20T12:00:00Z"
}
```

#### Failure Responses

**404 — Event Not Found (or INACTIVE for customer)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/events/999"
}
```

---

## 8. Seat Management (Admin)

---

### 8.1 `POST /api/v1/admin/events/{eventId}/seats`

**Auth:** ADMIN

#### Request

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

#### Success Response — `201 Created`

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
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 102,
      "eventId": 42,
      "seatNumber": "A2",
      "category": "VIP",
      "price": 199.99,
      "status": "AVAILABLE",
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 103,
      "eventId": 42,
      "seatNumber": "B1",
      "category": "STANDARD",
      "price": 79.99,
      "status": "AVAILABLE",
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 104,
      "eventId": 42,
      "seatNumber": "B2",
      "category": "STANDARD",
      "price": 79.99,
      "status": "AVAILABLE",
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    }
  ],
  "created": 4
}
```

#### Failure Responses

**400 — Validation Failed (empty seats array)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42/seats",
  "details": [
    { "field": "seats", "message": "At least one seat is required." }
  ]
}
```

**400 — Validation Failed (invalid seat fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42/seats",
  "details": [
    { "field": "seats[0].seatNumber", "message": "The 'seatNumber' field is required." },
    { "field": "seats[1].price", "message": "Price must be greater than 0." },
    { "field": "seats[2].category", "message": "The 'category' field is required." }
  ]
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/admin/events/999/seats"
}
```

**409 — Duplicate Seat Number**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "DuplicateSeatNumber",
  "message": "One or more seat numbers already exist for this event. The entire batch has been rejected.",
  "path": "/api/v1/admin/events/42/seats",
  "details": [
    { "field": "seats[0].seatNumber", "message": "Seat number 'A1' already exists for event id '42'." }
  ]
}
```

---

### 8.2 `PUT /api/v1/admin/events/{eventId}/seats/{seatId}`

**Auth:** ADMIN

#### Request

```json
{
  "category": "STANDARD",
  "price": 89.99,
  "status": "DISABLED"
}
```

#### Success Response — `200 OK`

```json
{
  "id": 101,
  "eventId": 42,
  "seatNumber": "A1",
  "category": "STANDARD",
  "price": 89.99,
  "status": "DISABLED",
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T15:00:00Z"
}
```

#### Failure Responses

**400 — Invalid Seat Status Update (tried to set BOOKED)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "InvalidSeatStatusUpdate",
  "message": "Seat status cannot be manually set to 'BOOKED'. Only the booking system can set this status.",
  "path": "/api/v1/admin/events/42/seats/101"
}
```

**400 — Validation Failed (invalid price)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/admin/events/42/seats/101",
  "details": [
    { "field": "price", "message": "Price must be greater than 0." }
  ]
}
```

**404 — Seat Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The seat with id '999' was not found for event '42'.",
  "path": "/api/v1/admin/events/42/seats/999"
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/admin/events/999/seats/101"
}
```

---

## 9. Seat Browsing (Shared)

---

### 9.1 `GET /api/v1/events/{eventId}/seats`

**Auth:** CUSTOMER / ADMIN

#### Request

No request body. Path parameter: `eventId`. Query parameters: `status`, `category` (both optional filters).

```
GET /api/v1/events/42/seats?status=AVAILABLE&category=VIP
```

> **No pagination** — seats for an event are fixed at creation time. All matching seats are returned in one response.

#### Success Response — `200 OK`

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
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 102,
      "eventId": 42,
      "seatNumber": "A2",
      "category": "VIP",
      "price": 199.99,
      "status": "AVAILABLE",
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    }
  ],
  "totalCount": 2
}
```

#### Success Response — `200 OK` (no matching seats)

```json
{
  "items": [],
  "totalCount": 0
}
```

#### Failure Responses

**403 — Event Not Active (customer accessing INACTIVE event seats)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "EventNotActive",
  "message": "Seats cannot be viewed for an inactive event.",
  "path": "/api/v1/events/42/seats"
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/events/999/seats"
}
```

---

## 10. Booking Management (Customer)

---

### 10.1 `POST /api/v1/bookings`

**Auth:** CUSTOMER

#### Request

```json
{
  "eventId": 42,
  "seatIds": [101, 102]
}
```

#### Success Response — `201 Created`

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "PENDING_PAYMENT",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    {
      "id": 15,
      "seatId": 101,
      "priceAtBooking": 199.99,
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
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Failure Responses

**400 — Validation Failed (missing fields)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/bookings",
  "details": [
    { "field": "eventId", "message": "The 'eventId' field is required." },
    { "field": "seatIds", "message": "The 'seatIds' field is required." }
  ]
}
```

**400 — Validation Failed (empty seatIds)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 400,
  "error": "ValidationFailed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/bookings",
  "details": [
    { "field": "seatIds", "message": "At least one seat ID is required." }
  ]
}
```

**404 — Event Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The event with id '999' was not found.",
  "path": "/api/v1/bookings"
}
```

**404 — Seat Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The seat with id '999' was not found for event '42'.",
  "path": "/api/v1/bookings"
}
```

**409 — Event Not Active**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "EventNotActive",
  "message": "Cannot create a booking for event '42' because it is not in ACTIVE status.",
  "path": "/api/v1/bookings"
}
```

**409 — Seat Unavailable (single seat)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "SeatUnavailable",
  "message": "One or more selected seats are not available. The entire booking has been rolled back.",
  "path": "/api/v1/bookings",
  "details": [
    { "field": "seatIds[0]", "message": "Seat 'A1' (id: 101) has status 'BOOKED' and cannot be reserved." }
  ]
}
```

**409 — Seat Unavailable (multiple seats)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "SeatUnavailable",
  "message": "One or more selected seats are not available. The entire booking has been rolled back.",
  "path": "/api/v1/bookings",
  "details": [
    { "field": "seatIds[0]", "message": "Seat 'A1' (id: 101) has status 'BOOKED' and cannot be reserved." },
    { "field": "seatIds[1]", "message": "Seat 'A2' (id: 102) has status 'DISABLED' and cannot be reserved." }
  ]
}
```

---

### 10.2 `GET /api/v1/bookings`

**Auth:** CUSTOMER

#### Request

No request body. Query parameters: `limit`, `offset`, `sort`.

```
GET /api/v1/bookings?limit=25&offset=0&sort=createdAt,desc
```

#### Success Response — `200 OK`

```json
{
  "items": [
    {
      "id": 7,
      "eventId": 42,
      "userId": 3,
      "status": "PENDING_PAYMENT",
      "totalAmount": 399.98,
      "bookingDate": "2026-04-04T14:30:00Z",
      "items": [
        { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
        { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
      ],
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:30:00Z"
    },
    {
      "id": 5,
      "eventId": 43,
      "userId": 3,
      "status": "CONFIRMED",
      "totalAmount": 79.99,
      "bookingDate": "2026-04-01T10:00:00Z",
      "items": [
        { "id": 12, "seatId": 201, "priceAtBooking": 79.99, "createdAt": "2026-04-01T10:00:00Z" }
      ],
      "createdAt": "2026-04-01T10:00:00Z",
      "updatedAt": "2026-04-01T10:05:00Z"
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 2
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 10.3 `GET /api/v1/bookings/{bookingId}`

**Auth:** CUSTOMER (owner) / ADMIN

#### Request

No request body. Path parameter: `bookingId`.

```
GET /api/v1/bookings/7
```

#### Success Response — `200 OK` (PENDING_PAYMENT — with action links)

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "PENDING_PAYMENT",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
    { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:30:00Z"
}
```

#### Success Response — `200 OK` (CONFIRMED — no action links)

```json
{
  "id": 5,
  "eventId": 43,
  "userId": 3,
  "status": "CONFIRMED",
  "totalAmount": 79.99,
  "bookingDate": "2026-04-01T10:00:00Z",
  "items": [
    { "id": 12, "seatId": 201, "priceAtBooking": 79.99, "createdAt": "2026-04-01T10:00:00Z" }
  ],
  "createdAt": "2026-04-01T10:00:00Z",
  "updatedAt": "2026-04-01T10:05:00Z"
}
```

#### Failure Responses

**403 — Forbidden (not the booking owner)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this booking. Only the booking owner or an admin can view it.",
  "path": "/api/v1/bookings/7"
}
```

**404 — Booking Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The booking with id '999' was not found.",
  "path": "/api/v1/bookings/999"
}
```

---

### 10.4 `POST /api/v1/bookings/{bookingId}/confirm`

**Auth:** CUSTOMER (owner)

#### Request

```json
{}
```

#### Success Response — `200 OK`

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "CONFIRMED",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
    { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:35:00Z"
}
```

#### Failure Responses

**403 — Forbidden (not the booking owner)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to confirm this booking. Only the booking owner can confirm it.",
  "path": "/api/v1/bookings/7/confirm"
}
```

**404 — Booking Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The booking with id '999' was not found.",
  "path": "/api/v1/bookings/999/confirm"
}
```

**409 — Invalid State Transition (already confirmed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot confirm a booking with current status 'CONFIRMED'. Only PENDING_PAYMENT bookings can be confirmed.",
  "path": "/api/v1/bookings/7/confirm"
}
```

**409 — Invalid State Transition (cancelled)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot confirm a booking with current status 'CANCELLED'. Only PENDING_PAYMENT bookings can be confirmed.",
  "path": "/api/v1/bookings/7/confirm"
}
```

**409 — Invalid State Transition (payment failed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot confirm a booking with current status 'PAYMENT_FAILED'. Only PENDING_PAYMENT bookings can be confirmed.",
  "path": "/api/v1/bookings/7/confirm"
}
```

---

### 10.5 `POST /api/v1/bookings/{bookingId}/cancel`

**Auth:** CUSTOMER (owner)

#### Request

```json
{}
```

#### Success Response — `200 OK`

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "CANCELLED",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
    { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:35:00Z"
}
```

#### Failure Responses

**403 — Forbidden (not the booking owner)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to cancel this booking. Only the booking owner can cancel it.",
  "path": "/api/v1/bookings/7/cancel"
}
```

**404 — Booking Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The booking with id '999' was not found.",
  "path": "/api/v1/bookings/999/cancel"
}
```

**409 — Invalid State Transition (already cancelled)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot cancel a booking with current status 'CANCELLED'. Only PENDING_PAYMENT bookings can be cancelled.",
  "path": "/api/v1/bookings/7/cancel"
}
```

**409 — Invalid State Transition (confirmed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot cancel a booking with current status 'CONFIRMED'. Only PENDING_PAYMENT bookings can be cancelled.",
  "path": "/api/v1/bookings/7/cancel"
}
```

**409 — Invalid State Transition (payment failed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot cancel a booking with current status 'PAYMENT_FAILED'. Only PENDING_PAYMENT bookings can be cancelled.",
  "path": "/api/v1/bookings/7/cancel"
}
```

---

### 10.6 `POST /api/v1/bookings/{bookingId}/payment-failed`

**Auth:** CUSTOMER / System

#### Request

```json
{}
```

#### Success Response — `200 OK`

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "PAYMENT_FAILED",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
    { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:35:00Z"
}
```

#### Failure Responses

**404 — Booking Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The booking with id '999' was not found.",
  "path": "/api/v1/bookings/999/payment-failed"
}
```

**409 — Invalid State Transition (already confirmed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot report payment failure for a booking with current status 'CONFIRMED'. Only PENDING_PAYMENT bookings can transition to PAYMENT_FAILED.",
  "path": "/api/v1/bookings/7/payment-failed"
}
```

**409 — Invalid State Transition (already cancelled)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot report payment failure for a booking with current status 'CANCELLED'. Only PENDING_PAYMENT bookings can transition to PAYMENT_FAILED.",
  "path": "/api/v1/bookings/7/payment-failed"
}
```

**409 — Invalid State Transition (already payment failed)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 409,
  "error": "InvalidStateTransition",
  "message": "Cannot report payment failure for a booking with current status 'PAYMENT_FAILED'. Only PENDING_PAYMENT bookings can transition to PAYMENT_FAILED.",
  "path": "/api/v1/bookings/7/payment-failed"
}
```

---

## 11. Booking Management (Admin)

---

### 11.1 `GET /api/v1/admin/bookings`

**Auth:** ADMIN

#### Request

No request body. Query parameters: `limit`, `offset`, `sort`, `status`, `eventId`, `userId`.

```
GET /api/v1/admin/bookings?limit=25&offset=0&sort=createdAt,desc&status=CONFIRMED&eventId=42
```

#### Success Response — `200 OK`

```json
{
  "items": [
    {
      "id": 7,
      "eventId": 42,
      "userId": 3,
      "status": "CONFIRMED",
      "totalAmount": 399.98,
      "bookingDate": "2026-04-04T14:30:00Z",
      "items": [
        { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
        { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
      ],
      "createdAt": "2026-04-04T14:30:00Z",
      "updatedAt": "2026-04-04T14:35:00Z"
    }
  ],
  "limit": 25,
  "offset": 0,
  "totalCount": 1
}
```

#### Failure Responses

> 401, 403 — See [Common Failure Responses](#common-failure-responses-applicable-to-all-secured-endpoints).

---

### 11.2 `GET /api/v1/admin/bookings/{bookingId}`

**Auth:** ADMIN

#### Request

No request body. Path parameter: `bookingId`.

```
GET /api/v1/admin/bookings/7
```

#### Success Response — `200 OK`

```json
{
  "id": 7,
  "eventId": 42,
  "userId": 3,
  "status": "CONFIRMED",
  "totalAmount": 399.98,
  "bookingDate": "2026-04-04T14:30:00Z",
  "items": [
    { "id": 15, "seatId": 101, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" },
    { "id": 16, "seatId": 102, "priceAtBooking": 199.99, "createdAt": "2026-04-04T14:30:00Z" }
  ],
  "createdAt": "2026-04-04T14:30:00Z",
  "updatedAt": "2026-04-04T14:35:00Z"
}
```

#### Failure Responses

**404 — Booking Not Found**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 404,
  "error": "ResourceNotFound",
  "message": "The booking with id '999' was not found.",
  "path": "/api/v1/admin/bookings/999"
}
```

---

## 12. Health Check

---

### 12.1 `GET /health`

**Auth:** Public

#### Request

No request body.

```
GET /health
```

#### Success Response — `200 OK`

```json
{
  "status": "UP",
  "timestamp": "2026-04-04T14:00:00Z"
}
```

#### Failure Responses

**500 — Internal Server Error (application unhealthy)**

```json
{
  "timestamp": "2026-04-04T14:30:00Z",
  "status": 500,
  "error": "InternalServerError",
  "message": "An unexpected server error occurred. Please try again later.",
  "path": "/health"
}
```

---

## Summary

| Section | Endpoints | Total Scenarios (Success + Failure) |
|---------|-----------|-------------------------------------|
| Auth | 2 | 2 success + 8 failure = **10** |
| User Management | 3 | 3 success + 5 failure = **8** |
| Role Management | 6 | 6 success + 12 failure = **18** |
| Permission Management | 5 | 5 success + 10 failure = **15** |
| Event Management (Admin) | 4 | 4 success + 10 failure = **14** |
| Event Browsing | 2 | 3 success + 1 failure = **4** |
| Seat Management (Admin) | 2 | 2 success + 7 failure = **9** |
| Seat Browsing | 1 | 2 success + 2 failure = **4** |
| Booking Management (Customer) | 6 | 8 success + 18 failure = **26** |
| Booking Management (Admin) | 2 | 2 success + 1 failure = **3** |
| Health Check | 1 | 1 success + 1 failure = **2** |
| **Total** | **34** | **113** |

> Common failure responses (401 Unauthorized, 403 Forbidden, 500 Internal Server Error) apply to **all secured endpoints** and are documented once in §1 to avoid repetition.
