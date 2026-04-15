# Auth API Implementation Guide

## Overview
Registration and login APIs have been implemented with JWT authentication, BCrypt password hashing, and role-based access control.

## Files Created

### DTOs (Data Transfer Objects)
- `dto/auth/RegisterRequest.java` - Request for user registration
- `dto/auth/LoginRequest.java` - Request for user login
- `dto/auth/LoginResponse.java` - Response containing JWT token and user details

### Services
- `service/AuthService.java` - Interface for auth operations
- `service/AuthServiceImpl.java` - Implementation with JWT token generation and password hashing

### Controllers
- `controller/AuthController.java` - REST endpoints for register and login

### Repositories
- `repository/RoleRepository.java` - Repository for Role entity queries

### Configuration
- `config/SecurityConfig.java` - Spring Security configuration with BCrypt encoder (strength 12)

### Exceptions
- `exception/DuplicateEmailException.java` - Thrown when email already exists
- `exception/InvalidCredentialsException.java` - Thrown on invalid login credentials
- Exception handlers added to `GlobalExceptionHandler.java`

## Configuration Updates

### application.yaml
```yaml
jwt:
  secret: your-super-secret-jwt-key-change-in-production-min-32-chars
  expiration: 86400000  # 24 hours in milliseconds
```

### pom.xml
Added JWT dependency:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.11.5</version>
</dependency>
```

## Setup Instructions

### 1. Create Bootstrap Data (one-time setup)

First, register an admin user via API:
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Bootstrap Admin",
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

Response: `201 Created` (no body)

Then, run the bootstrap SQL script to seed roles, permissions, and assign ADMIN role to the user:
```bash
mysql -u eventicketingsystemuser -p eventicketingsystem < GIVEN/bootstrap_admin_roles_permissions_mysql.sql
```

Or manually execute the SQL file in your MySQL client.

### 2. Verify Setup

Query to confirm admin was created and assigned ADMIN role:
```sql
SELECT u.id, u.email, r.name AS role_name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'admin@example.com';
```

## API Endpoints

### 1. POST /api/v1/auth/register
**Auth:** Public

Register a new user. User is auto-assigned CUSTOMER role.

**Request:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Success Response:** `201 Created` (no response body)

**Failure Responses:**
- `400 Bad Request` - Validation failed (missing fields, invalid email, password too short)
- `409 Conflict` - Email already registered

---

### 2. POST /api/v1/auth/login
**Auth:** Public

Authenticate user and receive JWT token.

**Request:**
```json
{
  "email": "jane@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Success Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjMsInJvbGVzIjpbIkNVU1RPTUVSIl0sImV4cCI6MTc3ODAwMTYwMH0.signature",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 3,
  "roles": ["CUSTOMER"]
}
```

**Failure Responses:**
- `400 Bad Request` - Validation failed (missing fields)
- `401 Unauthorized` - Invalid email or password

---

## Security Features

1. **Password Hashing**
   - Uses BCrypt with strength factor 12
   - Passwords are never stored in plain text

2. **JWT Token**
   - Contains userId, roles, and expiration time
   - Signed with HS512 algorithm
   - Default expiration: 24 hours
   - **IMPORTANT:** Change `jwt.secret` in production to a long, random string

3. **Auto-Role Assignment**
   - New users registering are automatically assigned CUSTOMER role
   - Admin promotion must be done manually via SQL or admin APIs (once implemented)

4. **Exception Handling**
   - All exceptions follow standard API error format
   - Proper HTTP status codes returned
   - Descriptive error messages for debugging

## Usage Flow

```
1. User registers via POST /api/v1/auth/register
   ↓
2. User receives 201 Created confirmation
   ↓
3. System auto-assigns CUSTOMER role
   ↓
4. User logs in via POST /api/v1/auth/login
   ↓
5. User receives JWT token in response
   ↓
6. User includes token in Authorization header: "Bearer <token>"
   ↓
7. User can now access protected endpoints
```

## Production Checklist

- [ ] Change `jwt.secret` in application.yaml to a secure, randomly generated key (min 32 chars)
- [ ] Consider using environment variables for sensitive config (jwt.secret, DB password)
- [ ] Run the bootstrap SQL script to seed initial roles and permissions
- [ ] Create first admin user via register API
- [ ] Verify admin has ADMIN role via SQL query
- [ ] Test register and login endpoints
- [ ] Enable HTTPS in production
- [ ] Implement token refresh mechanism if needed
- [ ] Add rate limiting to prevent brute force attacks


