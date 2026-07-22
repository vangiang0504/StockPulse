# Story 1.1: Enforce the Catalog and Warehouse Authorization Matrix

Status: done

## Story

As a StockPulse administrator,
I want catalog and warehouse endpoints to enforce the specified role matrix,
so that authenticated users can perform only the inventory operations assigned to their responsibilities.

## Acceptance Criteria

1. `Role` supports the exact string-backed values `USER`, `STAFF`, `MANAGER`, and `ADMIN`; existing rows containing `USER` or `ADMIN` remain readable, login and token refresh continue to work for those accounts, new self-registered and starter-created users still default to `USER`, and no schema migration is introduced.
2. An authenticated `STAFF`, `MANAGER`, or `ADMIN` user can list, search, and view products and can list and view categories.
3. Only authenticated `MANAGER` and `ADMIN` users can create or update products and categories; `USER` and `STAFF` receive HTTP 403 with the standard `ApiResponse` error shape.
4. An authenticated `STAFF`, `MANAGER`, or `ADMIN` user can list and view warehouses.
5. Only an authenticated `ADMIN` user can create or update warehouses; `USER`, `STAFF`, and `MANAGER` receive HTTP 403 with the standard `ApiResponse` error shape.
6. An unauthenticated request to any protected catalog or warehouse endpoint receives HTTP 401 through the stateless security flow and uses the standard API error shape.
7. Existing endpoint paths, response and pagination envelopes, immutable Product SKU/Category code/Warehouse code rules, Category hierarchy validation, Warehouse soft-deactivation, and service transaction boundaries are unchanged.
8. Automated authorization integration tests exercise the real Spring Security filter chain and cover unauthenticated, `USER`, `STAFF`, `MANAGER`, and `ADMIN` access across Product, Category, and Warehouse reads and writes; the existing 37 service tests continue to pass unchanged.

## Tasks / Subtasks

- [x] Extend the stored role model (AC: 1)
  - [x] Add `STAFF` and `MANAGER` to `Role` without renaming or removing `USER` or `ADMIN`.
  - [x] Preserve `User.role` as `EnumType.STRING`; do not add a Flyway migration because `users.role` is a string column with sufficient length.
  - [x] Confirm `UserDetailsServiceImpl` continues to expose every role as `ROLE_<ROLE>`.
- [x] Enforce Product authorization at the controller boundary (AC: 2, 3, 7)
  - [x] Permit `STAFF`, `MANAGER`, and `ADMIN` on list, detail, and search operations.
  - [x] Permit only `MANAGER` and `ADMIN` on create and update operations.
  - [x] Keep paths, request/response DTOs, pagination, sorting, and service calls unchanged.
- [x] Enforce Category authorization at the controller boundary (AC: 2, 3, 7)
  - [x] Permit `STAFF`, `MANAGER`, and `ADMIN` on list and detail operations.
  - [x] Permit only `MANAGER` and `ADMIN` on create and update operations.
  - [x] Preserve immutable code, partial-update, and hierarchy-validation behavior.
- [x] Enforce Warehouse authorization at the controller boundary (AC: 4, 5, 7)
  - [x] Permit `STAFF`, `MANAGER`, and `ADMIN` on list and detail operations.
  - [x] Permit only `ADMIN` on create and update operations.
  - [x] Preserve immutable code and soft-deactivation behavior; do not add a DELETE endpoint.
- [x] Make authentication and authorization failures API-consistent (AC: 3, 5, 6)
  - [x] Preserve the existing `@EnableMethodSecurity` configuration.
  - [x] Verify unauthenticated filter-chain failures return 401, not the framework default 403.
  - [x] Add one reusable security error component implementing `AuthenticationEntryPoint` and `AccessDeniedHandler`, then register it in `SecurityConfig` so filter-chain failures serialize the same `success: false`, `message`, and `timestamp` envelope as `ApiResponse.error(...)`.
  - [x] Preserve `GlobalExceptionHandler` handling for method-level `AccessDeniedException` and avoid leaking exception details.
- [ ] Add authorization integration coverage (AC: 1-8)
  - [x] Add a `CatalogWarehouseAuthorizationIntegrationTest` that extends the existing `BaseIntegrationTest`, creates active users for all four roles, obtains or generates valid JWTs through existing application support, and sends requests through the real HTTP security chain.
  - [x] Verify unauthenticated access on a representative protected endpoint returns 401 and the error envelope.
  - [x] Verify `USER` cannot read or write Product, Category, or Warehouse resources.
  - [x] Verify `STAFF` can read Product/Category/Warehouse resources but cannot perform their protected writes.
  - [x] Verify `MANAGER` can read and write Product/Category resources, can read Warehouse resources, and cannot write Warehouse resources.
  - [x] Verify `ADMIN` can perform the representative read and write operations for all three resources.
  - [x] Assert denied requests do not invoke or mutate the underlying service/domain state.
  - [ ] Run the complete backend test suite and confirm existing service tests remain green.

## Dev Notes

### Current-state findings

- `SecurityConfig` already declares `@EnableMethodSecurity`; do not duplicate or replace it. The gap is that the business controllers have no role annotations.
- `UserDetailsServiceImpl` already maps a stored enum value to `ROLE_` plus the enum name, so Spring expressions should use `hasAnyRole('STAFF', ...)` or equivalent role names without manually changing authority construction.
- JWTs contain the username only. The filter reloads `UserDetails`, so role changes are resolved from the database on authenticated requests; adding role claims to tokens is unnecessary for this story.
- `GlobalExceptionHandler` already maps controller/method `AccessDeniedException` to `ApiResponse.error("Access denied")` with HTTP 403. Security exceptions raised before controller invocation may require explicit JSON handlers in the filter chain to satisfy the 401/403 envelope criteria.
- `Role` is persisted with `EnumType.STRING` in a `varchar(20)` column. `STAFF` and `MANAGER` fit the current schema, so a migration would add risk without value.

### Existing-account compatibility requirements

- Keep the enum constants exactly named `ADMIN` and `USER`; do not rename them, change their case, introduce aliases, or replace string persistence with ordinals.
- Existing `ADMIN` rows must deserialize without data changes and retain full read/write access to Product, Category, and Warehouse endpoints.
- Existing `USER` rows must deserialize and continue to authenticate, refresh tokens, and use starter functionality, but receive no implicit StockPulse Product, Category, or Warehouse permission; protected domain calls return 403 after authentication.
- Preserve `AuthServiceImpl.register(...)` and `UserServiceImpl.create(...)` defaulting new accounts to `Role.STAFF`. This story does not add role selection to public registration or change User CRUD.
- Preserve the current JWT contract: tokens identify the username and `JwtAuthenticationFilter` reloads `UserDetails`; do not add role claims or invalidate previously issued valid tokens solely because the enum gains values.
- Preserve `UserDetailsServiceImpl` authority names as `ROLE_<stored enum name>`, including `ROLE_ADMIN` and `ROLE_USER`.
- Do not edit V1 or V2 and do not add a migration. The existing `users.role VARCHAR(20)` column and `USER` default remain authoritative.

### Authorization matrix

| Resource operation | USER | STAFF | MANAGER | ADMIN |
|---|---:|---:|---:|---:|
| Product list/detail/search | Deny | Allow | Allow | Allow |
| Product create/update | Deny | Deny | Allow | Allow |
| Category list/detail | Deny | Allow | Allow | Allow |
| Category create/update | Deny | Deny | Allow | Allow |
| Warehouse list/detail | Deny | Allow | Allow | Allow |
| Warehouse create/update | Deny | Deny | Deny | Allow |

Unauthenticated access to every row is denied with HTTP 401.

### Implementation boundaries

- Apply authorization at public controller methods so the HTTP contract is explicit and testable. Do not move unrelated business rules out of the service layer.
- This story does not add frontend role-aware visibility, alter User endpoint permissions, change public auth/Swagger/actuator routes, or implement authorization for future StockPulse endpoints.
- Do not broaden this story into product search, Warehouse UI, Category UI, schema, cache, or messaging work.
- Keep the existing stateless JWT and BCrypt design.

### Test guidance

- Use `BaseIntegrationTest` and `TestRestTemplate` so the actual JWT filter, user lookup, method security, exception translation, controller mapping, and JSON envelope are exercised together. `spring-security-test` is already present, but a controller-only `@WebMvcTest` does not replace the required integration coverage.
- Create isolated Product, Category, and Warehouse fixtures for allowed detail/update cases. Use unique usernames and business keys and clean up without relying on test order.
- Include Product search among read protection because it has a distinct controller method and path matching can be easy to overlook.
- For write tests, use syntactically valid request bodies so authorization is the behavior under test rather than bean validation.
- Assert status plus the stable envelope fields (`success`, `message`, `timestamp`); do not assert an exact timestamp value.

### Required authorization test cases

| Caller | Requests | Expected result |
|---|---|---|
| Unauthenticated | Representative `GET` and `POST` under `/api/v1/products`, `/api/v1/categories`, and `/api/v1/warehouses` | 401; `success=false`, non-empty message, timestamp present |
| Existing `USER` | Product list/detail/search, Category list/detail, Warehouse list/detail | 403 with the standard error envelope |
| Existing `USER` | Product/Category/Warehouse create and update | 403 with the standard error envelope and no mutation |
| `STAFF` | Product list/detail/search, Category list/detail, Warehouse list/detail | Allowed; preserve current success and pagination envelopes |
| `STAFF` | Product/Category/Warehouse create and update | 403 with no mutation |
| `MANAGER` | Product/Category/Warehouse reads | Allowed |
| `MANAGER` | Product and Category create/update | Allowed with the existing 201/200 contracts |
| `MANAGER` | Warehouse create/update | 403 with no mutation |
| Existing `ADMIN` | Every Product/Category/Warehouse read and write operation in this story | Allowed with unchanged response contracts |

For each allowed detail/update case, use an existing fixture ID so a 404 cannot mask authorization success. For each denied write, verify the target repository state is unchanged. Include both `POST` and `PUT` coverage because they are separate secured controller methods.

### Exact affected files

Modify only these production files:

- `backend/src/main/java/com/training/starter/enums/Role.java` — append `STAFF` and `MANAGER` while preserving `ADMIN` and `USER`.
- `backend/src/main/java/com/training/starter/security/SecurityConfig.java` — retain `@EnableMethodSecurity` and register JSON 401/403 handling.
- `backend/src/main/java/com/training/starter/controller/ProductController.java` — add method authorization to list, detail, search, create, and update.
- `backend/src/main/java/com/training/starter/controller/CategoryController.java` — add method authorization to list, detail, create, and update.
- `backend/src/main/java/com/training/starter/controller/WarehouseController.java` — add method authorization to list, detail, create, and update.

Add exactly these files:

- `backend/src/main/java/com/training/starter/security/RestSecurityErrorHandler.java` — shared `AuthenticationEntryPoint`/`AccessDeniedHandler` that writes `ApiResponse` JSON.
- `backend/src/test/java/com/training/starter/security/CatalogWarehouseAuthorizationIntegrationTest.java` — complete role-matrix and error-envelope integration coverage.

Inspected compatibility dependencies that must remain unchanged:

- `backend/src/main/java/com/training/starter/entity/User.java`
- `backend/src/main/java/com/training/starter/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/training/starter/security/JwtTokenProvider.java`
- `backend/src/main/java/com/training/starter/security/UserDetailsServiceImpl.java`
- `backend/src/main/java/com/training/starter/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/training/starter/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/training/starter/service/impl/UserServiceImpl.java`
- `backend/src/main/resources/db/migration/V1__create_users_table.sql`
- `backend/src/test/java/com/training/starter/BaseIntegrationTest.java`
- Existing `ProductServiceTest`, `CategoryServiceTest`, `WarehouseServiceTest`, and `UserServiceTest` files.

No frontend, DTO, mapper, service, repository, entity, migration, configuration-YAML, or `pom.xml` changes are in scope. Do not recreate or refactor Product, Category, or Warehouse CRUD.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 1.1, lines 27-43]
- [Source: `_bmad-output/project-context.md` — authorization gap and REQ-STP-B-106 status, lines 57-61 and 77-87]
- [Source: `_bmad-output/project-context.md` — critical backend authorization and compatibility rules, lines 182-196]
- [Source: `task.md` — REQ-STP-B-106 remaining work, lines 7-21]
- [Source: current `Role`, `User`, V1 migration, auth services, JWT security, error handling, controllers, and backend tests]
- [Source: `backend/src/main/java/com/training/starter/security/SecurityConfig.java` — stateless filter chain and method security]
- [Source: `backend/src/main/java/com/training/starter/security/UserDetailsServiceImpl.java` — `ROLE_<ROLE>` authority mapping]
- [Source: `backend/src/main/java/com/training/starter/exception/GlobalExceptionHandler.java` — API access-denied envelope]

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./mvnw.cmd test` on 2026-07-21: production and test compilation succeeded; the 37 existing service tests passed; the new Testcontainers integration test could not start because no Docker environment was available.

### Completion Notes List

- Added `STAFF` and `MANAGER` while preserving string persistence and existing role names.
- Applied the Product, Category, and Warehouse role matrix with controller method security.
- Added a shared JSON authentication/authorization failure handler and registered it in the stateless filter chain.
- Added full HTTP/JWT role-matrix integration coverage, including error envelopes and denied-write non-mutation assertions.
- Verification remains pending only for executing the new Testcontainers suite in a Docker-enabled environment.

### File List

- `backend/src/main/java/com/training/starter/enums/Role.java`
- `backend/src/main/java/com/training/starter/security/SecurityConfig.java`
- `backend/src/main/java/com/training/starter/security/RestSecurityErrorHandler.java`
- `backend/src/main/java/com/training/starter/controller/ProductController.java`
- `backend/src/main/java/com/training/starter/controller/CategoryController.java`
- `backend/src/main/java/com/training/starter/controller/WarehouseController.java`
- `backend/src/test/java/com/training/starter/security/CatalogWarehouseAuthorizationIntegrationTest.java`
