# Development and Testing

## Prerequisites

- JDK 17 or newer
- Maven 3.9+ or the included Maven wrapper
- Node.js 18+ and npm
- Docker with Docker Compose

## Local Startup

Start infrastructure from `backend`:

```powershell
docker compose up -d
docker compose ps
```

Run the backend on Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Run the frontend:

```powershell
cd frontend
npm install
npm start
```

The Angular development server proxies `/api` to `http://localhost:8080`.

## Configuration

`application.yml` provides environment-variable overrides:

| Setting | Environment variables/default |
|---|---|
| PostgreSQL | `DB_NAME=training_db`, `DB_USER=postgres`, `DB_PASS=postgres` |
| Redis | `REDIS_HOST=localhost`, `REDIS_PORT=6379` |
| RabbitMQ | `MQ_HOST=localhost`, `MQ_PORT=5672`, `MQ_USER=guest`, `MQ_PASS=guest` |
| Mail | `MAIL_HOST=localhost`, `MAIL_PORT=1025` |
| JWT | `JWT_SECRET`; access 900000 ms, refresh 604800000 ms |

The repository contains a development fallback JWT secret. Treat it as local-only and provide a strong external secret outside development.

## Backend Unit-Test Pattern

`UserServiceTest`, `ProductServiceTest`, `WarehouseServiceTest`, and `CategoryServiceTest` are the established examples:

- `@ExtendWith(MockitoExtension.class)`; no Spring context.
- Dependencies declared with `@Mock`.
- Service implementation created with `@InjectMocks`.
- Test names follow `method_condition_expectedResult`.
- Bodies use Given/When/Then comments.
- AssertJ checks results and exceptions.
- Mockito stubs repository/mapper behavior and verifies persistence side effects.
- Failure tests verify that forbidden saves/deletes never occur.
- Small private builders create representative entities/responses.

New StockPulse unit tests should cover every state transition and validation branch, not only successful persistence.

The newly merged service coverage includes:

- `WarehouseServiceTest` (8 tests): successful create, duplicate-code rejection, found/not-found lookup, successful/not-found update, soft deactivation with immutable code, and paginated listing.
- `CategoryServiceTest` (12 tests): child and root creation, duplicate-code and missing-parent rejection, found/not-found lookup, successful/not-found update, self-parent rejection, circular-hierarchy rejection, missing update parent, and paginated listing.
- These are Mockito unit tests of service behavior. They do not exercise controller request mapping, Bean Validation, generated MapStruct implementations, Flyway SQL, or PostgreSQL constraints.

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

## Integration-Test Infrastructure

`BaseIntegrationTest` provides:

- A random-port Spring Boot test context.
- PostgreSQL 16, Redis 7, and RabbitMQ Testcontainers.
- Dynamic property registration for container connection details.
- An injected `TestRestTemplate`.

No concrete integration test currently extends this base. StockPulse should use it for API/database/cache/messaging flows. Docker is required.

The test profile disables Flyway and uses Hibernate `create-drop`. That is appropriate for some integration tests but does not verify migration correctness. Add a separate Flyway-enabled integration test when validating V3+ migrations and PostgreSQL-specific objects such as the materialized view.

## Future StockPulse Test Requirements

- Service unit tests for CRUD duplicates/not-found cases and movement state transitions.
- Stock movement integration tests covering create -> approve -> complete -> persisted stock change.
- Concurrent export tests using independent transactions and real PostgreSQL locks; assert final stock and prove it never becomes negative.
- Redis tests for cache miss/hit, five-minute TTL configuration, and invalidation after committed movement completion.
- RabbitMQ tests for JSON event conversion, idempotent consumers, retries, and dead-letter routing.
- Materialized-view query/refresh tests against PostgreSQL.
- Backend authorization tests for STAFF, MANAGER, ADMIN, unauthenticated, and forbidden cases.

## Frontend Build and Tests

```powershell
cd frontend
npm run build
npm test
```

Angular uses Jasmine/Karma and strict TypeScript/template compilation. Add component/service tests for:

- Reactive form constraints and create/edit branching.
- Correct typed request payloads.
- Loading, empty, error, and success states.
- Server-side paginator interaction.
- Role/state-dependent action visibility.
- Notification and navigation behavior after mutations.

## Verification Before Delivery

For documentation-only changes, run Markdown/diff checks. For implementation changes, the proportional baseline is:

1. Backend unit tests.
2. Backend integration tests for affected infrastructure/domain flows.
3. Frontend production build.
4. Frontend tests for affected services/components.
5. Swagger inspection when endpoints or DTOs change.
6. Flyway startup against a clean PostgreSQL database when migrations change.

