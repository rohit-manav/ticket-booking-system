# AuthController Flow Diagram

```mermaid
flowchart TD
    A[Client] -->|POST /api/v1/auth/register| B[AuthController.register]
    B -->|Validate request| C{Email exists?}
    C -->|Yes| D[409 DuplicateEmail]
    C -->|No| E[Create User]
    E --> F[Assign CUSTOMER role]
    F --> G[Hash password with BCrypt]
    G --> H[Persist user]
    H --> I[201 Created]

    A -->|POST /api/v1/auth/login| J[AuthController.login]
    J -->|Validate request| K[Load user by email]
    K -->|Not found| L[401 InvalidCredentials]
    K -->|Found| M[Verify password]
    M -->|Invalid| L
    M -->|Valid| N[Collect roles]
    N --> O[Generate JWT sub=userId roles exp]
    O --> P[Return LoginResponse]

    P --> Q[Client stores accessToken]
    Q --> R[Authorization: Bearer <token>]
    R --> S[Access protected endpoints]
```

## Diagram 1: Classes Involved

```mermaid
classDiagram
    class AuthController
    class AuthService
    class AuthServiceImplementation
    class UserRepository
    class RoleRepository
    class PasswordEncoder
    class JwtTokenProvider
    class User
    class Role
    class LoginRequest
    class LoginResponse
    class RegisterRequest

    AuthController --> AuthService
    AuthService <|.. AuthServiceImplementation
    AuthServiceImplementation --> UserRepository
    AuthServiceImplementation --> RoleRepository
    AuthServiceImplementation --> PasswordEncoder
    AuthServiceImplementation --> JwtTokenProvider
    AuthServiceImplementation --> User
    AuthServiceImplementation --> Role
    AuthController --> LoginRequest
    AuthController --> LoginResponse
    AuthController --> RegisterRequest
```

## Diagram 2: Layered View (Controller -> Service -> Repository)

```mermaid
flowchart LR
    subgraph Controller
        AC[AuthController]
    end
    subgraph Service
        AS[AuthServiceImplementation]
    end
    subgraph Repository
        UR[UserRepository]
        RR[RoleRepository]
    end
    subgraph Security
        PE[PasswordEncoder]
        JWT[JwtTokenProvider]
    end

    AC -->|register/login| AS
    AS -->|register: existsByEmail, save| UR
    AS -->|register: findByName CUSTOMER| RR
    AS -->|register/login: hash or verify| PE
    AS -->|login: create JWT| JWT
```

Notes:
- Login returns `accessToken` and `tokenType` (current implementation).
- JWT subject (`sub`) is the user id; `roles` claim is included in the token.
- Error responses follow the common error schema in `GIVEN/api-request-response-schemas.md`.
- This layered view shows the main collaborators used by register/login.
- The service depends on repositories for user/role data and on security helpers for password hashing and JWT creation.
