# StockPulse Project Context

Generated from the repository state on 2026-07-21 after the Warehouse and Category CRUD merge. This is the implementation guide and requirements-status snapshot for AI agents working in this repository. It is not a PRD and does not prescribe a future architecture.

## Source-of-Truth Order

When sources conflict, apply this order:

1. Current executable source code, migrations, and tests for implementation status.
2. `Project 4 - StockPulse.md` version 1.1 for target requirements and accepted specification decisions.
3. `_bmad-output/project-documentation/` for established implementation patterns.

An active story or explicit user instruction may intentionally change the target. Do not claim a requirement is complete merely because a table, interface, configuration bean, placeholder, or documentation entry exists; verify the required behavior through its relevant layers.

## Status Definitions

- **IMPLEMENTED**: The current source contains the behavior required by the requirement, with its material layers present. Required tests are present when the requirement itself calls for them.
- **PARTIALLY_IMPLEMENTED**: A meaningful part exists, but at least one explicit capability, contract, or verification item is missing.
- **NOT_IMPLEMENTED**: The required domain behavior is absent. Generic starter infrastructure does not count as implementation of a StockPulse requirement.

## Current Implementation Snapshot

### Implemented application scope

- JWT authentication and User CRUD are inherited from the training starter.
- Product backend CRUD/search and Angular list/form flows exist.
- Warehouse backend list, detail, create, and partial update exist under `/api/v1/warehouses`.
- Category backend list, detail, create, and partial update exist under `/api/v1/categories`.
- Warehouse and Category have entities, request/response DTOs, MapStruct mappers, repositories, service interfaces, transactional service implementations, controllers, V2 schema, and unit tests.
- V2 creates `warehouses`, `categories`, and `products`; both Warehouse and Category inherit `createdAt` and `updatedAt` from `BaseEntity`, and both tables contain `created_at` and `updated_at`.
- The backend test suite currently passes 37 tests: 8 Warehouse, 12 Category, 10 Product, and 7 User service tests.

### Not yet implemented

- Warehouse and Category Angular features.
- Stock levels, movements, alerts, reorder suggestions, stock reporting, and their APIs/UI/tests.
- V3 through V7 migrations.
- Stock-specific Redis caching, RabbitMQ event flows, retries/DLQs, MailHog consumers, and scheduled materialized-view refresh.
- `STAFF` and `MANAGER` roles or endpoint-level role authorization.
- Active PostgreSQL full-text product search.

## Accepted v1.1 Specification Decisions

- **Audit timestamps:** Warehouse and Category have `updated_at`. Their entities extend `BaseEntity`, which sets `updatedAt` on create and update.
- **Immutable business keys:** Warehouse `code` and Product `sku` cannot be changed after creation. They are absent from update DTOs and explicitly ignored by update mappers.
- **Warehouse soft deactivation:** Warehouse has no DELETE endpoint. Retirement uses `PUT /api/v1/warehouses/{id}` with `active: false`.
- **Partial update semantics:** Nullable update-DTO fields mean “leave unchanged.” MapStruct update methods use `NullValuePropertyMappingStrategy.IGNORE`.
- **Category endpoints required:** The required Category API is list, detail, create, and update under `/api/v1/categories`; these endpoints now exist.
- **Plain ID foreign keys:** `Product.categoryId` and `Category.parentId` remain `Long` values rather than JPA relationships.

Consequences of null-ignore semantics:

- `UpdateWarehouseRequest.address = null` cannot clear an existing address.
- `UpdateCategoryRequest.parentId = null` cannot turn a child category into a root category.
- If explicit clearing is required later, introduce a deliberate API contract rather than changing null semantics silently.

## Known Gaps That Must Remain Visible

### Authorization gap

`Role` contains only `ADMIN` and `USER`. There is no `@PreAuthorize`, and `SecurityConfig` requires authentication without enforcing the v1.1 endpoint roles. The specification’s `STAFF`, `MANAGER`, and `ADMIN` authorization matrix is target behavior, not current behavior.

### PostgreSQL full-text search gap

`ProductRepository.searchByVector()` exists, and `products.search_vector` exists in V2, but `ProductServiceImpl.search()` calls `findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase()`. No trigger populates `search_vector`, and the V6 GIN index is absent. Current search is case-insensitive SKU/name substring matching, not PostgreSQL full-text search.

### API and domain boundary gaps

- Product SKU, Warehouse code, and Category code are immutable in update flows.
- `ProductService.delete()` and its tests exist, but `ProductController` has no DELETE endpoint, consistent with the v1.1 Products API table.
- Warehouse soft deactivation exists; Category has no delete/deactivation operation.
- Category parent validation prevents self-parenting and cycles, but null-ignore semantics cannot explicitly clear a parent.
- Controller pagination accepts client-provided `sortBy` values directly. Validate allowed sort fields before treating this as hardened input handling.

## REQ-STP Requirement Status Matrix

### Week 1 — Backend

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-B-101 | **IMPLEMENTED** | `Warehouse` contains name, code, address, and active fields and inherits id/timestamps; V2 creates `warehouses` with matching columns. |
| REQ-STP-B-102 | **IMPLEMENTED** | `Category` contains name, code, and nullable `parentId`; V2 creates the self-referencing `categories.parent_id` foreign key. |
| REQ-STP-B-103 | **IMPLEMENTED** | `Product` and V2 contain the specified SKU, descriptive, category, unit, threshold, reorder, active, and search-vector fields. |
| REQ-STP-B-104 | **IMPLEMENTED** | Warehouse and Category have list/detail/create/update service operations, transactional implementations, MapStruct mapping, validation, duplicate checks, and not-found handling. Warehouse retirement follows the accepted soft-deactivation contract. |
| REQ-STP-B-105 | **PARTIALLY_IMPLEMENTED** | Product CRUD service behavior and SKU/name search exist, but search uses `LIKE`-style case-insensitive matching rather than the required PostgreSQL full-text vector path. |
| REQ-STP-B-106 | **PARTIALLY_IMPLEMENTED** | Paginated Product, Warehouse, and Category REST endpoints exist, including the newly explicit Category endpoints. Required STAFF/MANAGER/ADMIN authorization is not enforced. |
| REQ-STP-B-107 | **NOT_IMPLEMENTED** | Only V1 and V2 exist. V3 stock levels, V4 movements/items, V5 alerts/reorder suggestions, V6 indexes, and V7 materialized view are absent. |

### Week 1 — Angular

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-F-101 | **PARTIALLY_IMPLEMENTED** | `ProductListComponent` has a paginated Material table with SKU, name, category ID, unit, status, and actions, but omits the required Min Stock and Reorder Point columns. |
| REQ-STP-F-102 | **PARTIALLY_IMPLEMENTED** | `ProductFormComponent` supports reactive create/edit and the specified fields; SKU is disabled during edit. Category is a numeric input, not the required category dropdown. |
| REQ-STP-F-103 | **NOT_IMPLEMENTED** | No `WarehouseListComponent` or Warehouse feature directory exists. |
| REQ-STP-F-104 | **PARTIALLY_IMPLEMENTED** | Product list/create/edit routes and sidebar navigation exist. Warehouse routes and navigation do not. |

### Week 1 — Testing

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-T-101 | **IMPLEMENTED** | `ProductServiceTest` has 10 passing tests, including valid create, duplicate SKU, get-by-ID, update, and search. |
| REQ-STP-T-102 | **IMPLEMENTED** | `WarehouseServiceTest` has 8 passing tests, including create and paginated get-all, plus detail, update, duplicate, not-found, and deactivation coverage. |
| REQ-STP-T-103 | **PARTIALLY_IMPLEMENTED** | OpenAPI configuration plus `@Tag`/`@Operation` annotations exist for current controllers, but there is no automated or recorded verification that Swagger UI exposes every required endpoint correctly. |

### Week 2 — Backend

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-B-201 | **NOT_IMPLEMENTED** | No StockLevel entity, migration, repository, service, or low-stock query exists. |
| REQ-STP-B-202 | **NOT_IMPLEMENTED** | No StockMovement/StockMovementItem entities or movement service exists. |
| REQ-STP-B-203 | **NOT_IMPLEMENTED** | No movement completion, ordered pessimistic locking, batch quantity update, or negative-stock guard exists. |
| REQ-STP-B-204 | **NOT_IMPLEMENTED** | No `/api/v1/stock`, `/stock/summary`, or `/stock/low` controller endpoints exist. |
| REQ-STP-B-205 | **NOT_IMPLEMENTED** | No movement REST controller or required import/export/transfer/approve/complete endpoints exist. |
| REQ-STP-B-206 | **NOT_IMPLEMENTED** | Redis connectivity is configured, but no `stock:{warehouseId}:{productId}` cache, five-minute TTL, or movement-driven invalidation exists. |
| REQ-STP-B-207 | **NOT_IMPLEMENTED** | A generic starter RabbitMQ topology (`training.*`) exists, but `stock.exchange`, the required queues/routing, and `StockEventPublisher` do not. |
| REQ-STP-B-208 | **NOT_IMPLEMENTED** | No `StockUpdateConsumer` or `StockLowEvent` publication flow exists. |
| REQ-STP-B-209 | **NOT_IMPLEMENTED** | No reorder consumer, email consumer, reorder-suggestion creation, or low-stock MailHog flow exists. |

### Week 2 — Angular

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-F-201 | **NOT_IMPLEMENTED** | No stock-level component exists. |
| REQ-STP-F-202 | **NOT_IMPLEMENTED** | No import/export movement form or dynamic item-row UI exists. |
| REQ-STP-F-203 | **NOT_IMPLEMENTED** | No movement list or type/status filtering UI exists. |
| REQ-STP-F-204 | **NOT_IMPLEMENTED** | No movement detail or role/state action UI exists. |

### Week 2 — Testing

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-T-201 | **NOT_IMPLEMENTED** | No StockMovementService or movement service tests exist. |
| REQ-STP-T-202 | **NOT_IMPLEMENTED** | No stock-cache hit or invalidation tests exist. |

### Week 3 — Backend

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-B-301 | **NOT_IMPLEMENTED** | `BaseIntegrationTest` configures containers, but no import/approve/complete movement integration test exists. |
| REQ-STP-B-302 | **NOT_IMPLEMENTED** | No concurrent export test or implemented stock locking exists. |
| REQ-STP-B-303 | **NOT_IMPLEMENTED** | No materialized view, unique index, scheduler, or concurrent refresh exists. |
| REQ-STP-B-304 | **NOT_IMPLEMENTED** | No post-completion low-stock evaluation, StockAlert, or ReorderSuggestion implementation exists. |
| REQ-STP-B-305 | **NOT_IMPLEMENTED** | No StockPulse retry policy or dead-letter topology exists. |
| REQ-STP-B-306 | **NOT_IMPLEMENTED** | No recorded `EXPLAIN ANALYZE` results or stock-query index verification exists. |
| REQ-STP-B-307 | **PARTIALLY_IMPLEMENTED** | Current Auth/User/Product/Warehouse/Category endpoints have basic `@Tag` and `@Operation` annotations. Future StockPulse endpoints and complete response/error documentation are absent. |

### Week 3 — Angular

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-F-301 | **NOT_IMPLEMENTED** | No alert list, severity display, or acknowledge action exists. |
| REQ-STP-F-302 | **NOT_IMPLEMENTED** | No reorder-suggestion list or approval UI exists. |
| REQ-STP-F-303 | **NOT_IMPLEMENTED** | The current dashboard is starter content; no StockPulse summary cards or stock metrics exist. |

### Week 3 — Testing

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-T-301 | **NOT_IMPLEMENTED** | Testcontainers scaffolding exists, but there are no executable integration tests for product CRUD, movement lifecycle, stock queries, or alerts. |
| REQ-STP-T-302 | **NOT_IMPLEMENTED** | No concurrent stock-locking test exists. |
| REQ-STP-T-303 | **NOT_IMPLEMENTED** | No RabbitMQ retry-to-DLQ test exists. |

### Week 4

| Requirement | Status | Source-backed assessment |
|---|---|---|
| REQ-STP-W-401 | **NOT_IMPLEMENTED** | The stock, movement, alert, reorder, and email portions of the end-to-end flow are absent. |
| REQ-STP-W-402 | **NOT_IMPLEMENTED** | No repository evidence records the required Team 1 cross-review with at least 10 comments. |
| REQ-STP-W-403 | **NOT_IMPLEMENTED** | No repository evidence identifies at least five mentor-issue bug fixes as satisfying this requirement. |
| REQ-STP-W-404 | **NOT_IMPLEMENTED** | No `ab -n 1000 -c 50` result or performance acceptance evidence exists. |
| REQ-STP-W-405 | **PARTIALLY_IMPLEMENTED** | `README.md` contains starter setup, services, extension guidance, and structure, but it is not a complete StockPulse README with current architecture and API documentation. |
| REQ-STP-W-406 | **NOT_IMPLEMENTED** | No repository evidence records preparation or delivery of the required 15–20 minute demo. |

### Status Totals

- **IMPLEMENTED:** 6
- **PARTIALLY_IMPLEMENTED:** 8
- **NOT_IMPLEMENTED:** 34
- **Total classified:** 48

## Critical Backend Implementation Rules

- Keep code under `com.training.starter` and follow controller → service interface → service implementation → repository layering.
- Controllers handle HTTP concerns; business rules and transaction boundaries belong in service implementations.
- Use constructor injection through `@RequiredArgsConstructor`.
- Use Java records for request/response DTOs and Bean Validation on request components.
- Use MapStruct with `componentModel = "spring"`; update methods use `@MappingTarget` and null-ignore semantics.
- Never expose JPA entities from controllers. Wrap results in `ApiResponse<T>` and pagination in `PageResponse<T>`.
- Keep endpoints under `/api/v1/<plural-resource>` and use zero-based Spring Data pagination.
- Flyway owns schema changes. Add sequential immutable migrations; do not rely on Hibernate schema creation.
- Keep JPA constraints and field mappings aligned with migration SQL.
- Treat Warehouse code and Product SKU as immutable. Do not add them to update DTOs.
- Retire Warehouses by setting `active = false`; do not add hard deletion without changing the accepted specification.
- Preserve Category self-parent and ancestry-cycle validation.
- Add backend method authorization when implementing `STAFF` and `MANAGER`; UI visibility is not authorization.

## Critical Frontend Implementation Rules

- Use Angular standalone components; do not introduce feature NgModules.
- Put domain features under `frontend/src/app/features/<feature>/`, shared UI under `shared/components`, and cross-cutting code under `core`.
- Lazy-load routed components with `loadComponent`; protected routes remain behind `authGuard` under `MainLayoutComponent`.
- Centralize HTTP in typed feature services and build URLs from `environment.apiUrl`.
- Mirror `ApiResponse<T>` and `PageResponse<T>` rather than consuming raw payloads.
- Use reactive forms with validators matching backend constraints. Keep immutable SKU disabled during product edit.
- Provide loading, empty, error, and success behavior for data-driven screens.
- Use server-side pagination for large lists and preserve zero-based indices.
- Avoid `any` and model nullable/optional values explicitly under strict TypeScript.

## Testing and Verification Rules

- Service tests use JUnit 5, Mockito, AssertJ, `@ExtendWith(MockitoExtension.class)`, and Given/When/Then organization.
- Test names follow `method_condition_expectedResult` and cover success, failure, and repository side effects.
- Integration tests should extend `BaseIntegrationTest`, but the base class alone does not satisfy an integration-test requirement.
- Stock concurrency tests must use real parallel transactions and prove persisted stock never becomes negative.
- Messaging tests must cover idempotent redelivery plus retry-to-DLQ behavior.
- Frontend tests should cover form validation, service interaction, page/error states, and role/state-dependent actions.
- Run backend tests with `backend/mvnw.cmd test` from the repository root on Windows or `./mvnw test` inside `backend` on Unix-like systems.
- Run Angular build/tests from `frontend` using the existing npm scripts.

## Domain Invariants for Future Stock Work

These are target requirements, not evidence that the corresponding features exist:

- SKU, Warehouse code, and movement reference are unique.
- Stock quantities and thresholds cannot be negative; movement item quantity is positive; reorder quantity is at least one.
- Available stock is `quantity - reservedQuantity` and cannot become negative.
- Transfers update source and destination atomically.
- Only movement completion changes stock; completion must be idempotent.
- Lock affected stock rows in ascending product-ID order during completion.
- Cache keys use `stock:{warehouseId}:{productId}` with a five-minute TTL and are invalidated after committed stock changes.
- Low-stock evaluation uses product-specific reorder configuration.
- Alerts and reorder suggestions must not duplicate an unresolved product/warehouse condition.
- The materialized view is reporting data, not the transactional source of truth.

## Technology Baseline

- Backend: Java 17, Spring Boot 3.2.5, Maven wrapper, Spring MVC/Data JPA/Security/Validation/AMQP/Redis/Mail, PostgreSQL 16, Flyway, MapStruct 1.5.5.Final, Lombok 1.18.32, JJWT 0.12.5, SpringDoc OpenAPI 2.3.0.
- Backend tests: JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers 1.19.7.
- Frontend: Angular 17.3 standalone components, TypeScript 5.4 strict mode, Angular Material 17.3, RxJS 7.8, Jasmine/Karma.
- Local endpoints: backend `http://localhost:8080`, frontend `http://localhost:4200`, application API prefix `/api/v1`.
- Local infrastructure is defined in `backend/docker-compose.yml`; use environment-variable overrides from `application.yml` and do not hard-code credentials or hosts.
