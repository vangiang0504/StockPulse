# Architecture

## Runtime Components

```text
Browser
  -> Angular 17 application (:4200 in development)
      -> /api proxy
          -> Spring Boot API (:8080)
              -> PostgreSQL (:5433 host / :5432 container)
              -> Redis (:6379)
              -> RabbitMQ (:5672; management :15672)
              -> MailHog SMTP (:1025; UI :8025)
```

Docker Compose provisions infrastructure only. The Spring Boot and Angular applications run separately during development.

## Backend Layering

The established request path is:

```text
Controller
  -> Service interface
      -> Service implementation (@Transactional)
          -> Spring Data repository
              -> PostgreSQL

Service implementation
  <-> MapStruct mapper
  <-> request/response records
```

Responsibilities are consistently separated:

- Controllers define paths, HTTP status, validation entry points, paging/sorting arguments, OpenAPI summaries, and `ApiResponse` wrapping.
- Service implementations own duplicate checks, not-found handling, password hashing, defaults, persistence, and transaction boundaries.
- `CategoryServiceImpl` also owns parent existence and cycle validation; this domain rule remains outside the controller and repository.
- Repositories extend `JpaRepository` and add derived or explicit queries.
- MapStruct converts between JPA entities and API DTOs.
- `GlobalExceptionHandler` converts expected exceptions to consistent error envelopes.

This section documents the layering present in the current code. Stock concurrency, movement state transitions, cache invalidation, and event publication are future requirements and are not designed in this brownfield reference.

## Backend Cross-Cutting Architecture

### Response Envelopes

Every non-204 API response uses `ApiResponse<T>`:

- `success`
- optional `message`
- optional `data`
- `timestamp`

Paged responses wrap data in `PageResponse<T>` with `content`, zero-based `page`, `size`, `totalElements`, `totalPages`, and `last`.

### Error Handling

`GlobalExceptionHandler` maps:

- `ResourceNotFoundException` -> 404
- `DuplicateResourceException` -> 409
- `BadRequestException` and validation/data-integrity errors -> 400
- `BadCredentialsException` -> 401
- `AccessDeniedException` -> 403
- unexpected exceptions -> 500 with a generic client message and server-side logging

### Persistence

- Flyway defines production schema state.
- Hibernate validates the schema and has Open Session in View disabled.
- `BaseEntity` supplies identity and lifecycle-managed creation/update timestamps.
- JPA batching is configured with a batch size of 50 plus ordered inserts and updates.

## Security Flow

```text
Angular login/register
  -> /api/v1/auth/*
      -> AuthenticationManager / UserRepository
          -> JwtTokenProvider
              -> access + refresh tokens

Subsequent Angular request
  -> jwtInterceptor adds Authorization: Bearer <token>
      -> JwtAuthenticationFilter validates token
          -> UserDetailsServiceImpl loads active user and ROLE_<role>
              -> SecurityContext
```

The backend is stateless and CSRF is disabled. Public paths are authentication, Swagger/OpenAPI, and actuator patterns; every other path requires authentication. CORS currently allows the Angular development origin only.

The current system creates Spring authorities but does not use method-level role checks on User, Product, Warehouse, or Category operations. StockPulse must add explicit backend authorization for STAFF/MANAGER/ADMIN operations after its role model is migrated.

## Frontend Architecture

- `app.config.ts` registers the router, HTTP client, functional JWT interceptor, and animations.
- `app.routes.ts` lazy-loads standalone components.
- Authenticated feature routes are children of `MainLayoutComponent` and protected by the functional `authGuard`.
- Login and registration are children of `AuthLayoutComponent`.
- Feature services own HTTP calls and return typed `Observable<ApiResponse<...>>` values.
- Components own presentation state, reactive forms, notifications, and navigation.
- Shared components provide confirmation, loading, and pagination primitives.

## Future Requirements Boundary

The project brief lists stock movements, stock-level locking, Redis caching, RabbitMQ workflows, alerts, reorder suggestions, email, and a materialized reporting view. None of those domain flows is currently implemented. This document records that boundary without proposing their future architecture.

