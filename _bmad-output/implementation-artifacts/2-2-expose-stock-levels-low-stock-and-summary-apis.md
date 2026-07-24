# Story 2.2: Expose Stock Levels, Low Stock, and Summary APIs

Status: done

## Story

As an authorized StockPulse staff member,
I want paginated stock-level, low-stock, and reporting-summary APIs,
so that I can inspect current inventory and identify replenishment needs from reliable data sources.

## Acceptance Criteria

1. A `StockLevel` JPA entity maps the released V3 `stock_levels` table exactly, using plain `Long productId` and `Long warehouseId` foreign-key fields, an optimistic-lock `@Version`, and its own `updatedAt` mapping. It does not extend `BaseEntity`, add `createdAt`, introduce JPA relationships, or change V3.
2. `StockLevelRepository` and `StockLevelService` provide a read-only lookup by `(warehouseId, productId)`. A missing row throws `ResourceNotFoundException`, and `availableQuantity` is always returned as `quantity - reservedQuantity`.
3. `GET /api/v1/stock` returns `ApiResponse<PageResponse<StockLevelResponse>>`, uses zero-based pagination, and supports optional `warehouseId` and `productId` filters without loading or exposing JPA relationships.
4. Stock-list responses contain the IDs plus product SKU/name and warehouse code/name required by the future stock-level screen, along with quantity, reserved quantity, available quantity, version, and updated timestamp.
5. `GET /api/v1/stock/low` returns a zero-based paginated response using the product-specific condition `stock_levels.quantity <= products.reorder_point`. It supports an optional `warehouseId` filter and returns the same display data plus the applicable reorder point. It does not use the fixed partial-index predicate as the business rule.
6. `GET /api/v1/stock/summary` returns a zero-based paginated `ApiResponse<PageResponse<StockSummaryResponse>>` sourced only from `mv_stock_summary`. It supports optional `warehouseId` and `productId` filters and maps every view projection, including `availableQuantity` and `stockStatus`.
7. Summary status values remain the V7 contract: quantity `= 0` is `OUT_OF_STOCK`, quantity `<= minStock` is `LOW_STOCK`, quantity `>= maxStock` is `OVERSTOCK`, otherwise `NORMAL`. The materialized view is reporting data and is never used for transactional stock lookup, validation, or future movement completion.
8. All three HTTP endpoints require one of `STAFF`, `MANAGER`, or `ADMIN` through backend method authorization. Missing authentication returns 401 and an authenticated authority outside the allowed matrix returns 403.
9. Service unit tests cover exact lookup found/not-found behavior, available-quantity mapping, filter delegation, and product-specific low-stock delegation. PostgreSQL integration tests cover filters, zero-based pagination, derived availability, per-product reorder thresholds, and summary-view mapping. Controller/security tests cover the response envelopes and role matrix.
10. This story is read-only backend scope. It does not add or modify Flyway migrations, refresh the materialized view, add Redis caching, add movement writes/locks, publish events, or add frontend screens.

## Tasks / Subtasks

- [x] Map the released stock schema (AC: 1, 2)
  - [x] Create `StockLevel` with explicit mappings for `id`, `productId`, `warehouseId`, `quantity`, `reservedQuantity`, `version`, and `updatedAt`.
  - [x] Use `@Version` on `version`; keep both foreign keys as `Long`.
  - [x] Do not extend `BaseEntity`, because V3 intentionally has no `created_at`.
  - [x] Do not edit V1-V7 or create a new migration for this read-only story.
- [x] Define typed read models (AC: 2-7)
  - [x] Add `StockLevelResponse` for transactional stock reads and `StockSummaryResponse` for materialized-view reads.
    - [x] Add `StockLevelResponse` for transactional stock reads.
    - [x] Add `StockSummaryResponse` for materialized-view reads.
  - [x] Include product and warehouse display fields required by REQ-STP-F-201.
  - [x] Represent `stockStatus` with a stable string-compatible enum or validated string matching V7 values.
  - [x] Keep entities out of the service response boundary.
- [x] Add repository query boundaries (AC: 2-7)
  - [x] Add exact `(warehouseId, productId)` lookup for the Story 2.3 cache boundary.
  - [x] Add one pageable stock-list query with nullable exact-match filters and joins for display data.
  - [x] Add one pageable low-stock query using each joined Product's `reorderPoint`, with an optional Warehouse filter.
  - [x] Add one pageable native query over `mv_stock_summary`, including a correct `countQuery` and optional exact-match filters.
  - [x] Use typed interface projections or explicit DTO projections; do not introduce `@ManyToOne`.
- [x] Implement the read-only service layer (AC: 2-7)
  - [x] Add `StockLevelService` and `StockLevelServiceImpl`.
  - [x] Mark the exact lookup, pageable list, low-stock query, and summary query `@Transactional(readOnly = true)`.
  - [x] Throw `ResourceNotFoundException` for an absent exact lookup using a clear product/warehouse message.
  - [x] Map transactional and reporting projections separately so the materialized view cannot become a write authority.
  - [x] Do not add cache annotations or `RedisTemplate` calls; Story 2.3 owns caching.
- [x] Expose authorized stock endpoints (AC: 3, 5, 6, 8)
  - [x] Add `StockController` at `/api/v1/stock` with `@Tag`, `@SecurityRequirement`, and `@Operation` documentation.
  - [x] Add paginated `GET /stock` with optional `warehouseId` and `productId`.
  - [x] Add paginated `GET /stock/low` with optional `warehouseId`.
  - [x] Add paginated `GET /stock/summary` with optional `warehouseId` and `productId`.
  - [x] Apply `@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")` to every operation.
    - [x] Apply the accepted role matrix to `GET /stock`.
    - [x] Apply the accepted role matrix to `GET /stock/low`.
    - [x] Apply the accepted role matrix to the summary operation.
  - [x] Use existing pagination defaults `page=0`, `size=20`; allow-list supported sort properties before repository execution.
  - [x] Wrap the implemented successful page with `ApiResponse.success(PageResponse.from(...))`.
- [x] Add focused unit and web/security coverage (AC: 2-9)
  - [x] Add Mockito service tests with Given/When/Then structure and strict stubbing.
    - [x] Add exact lookup and pageable-list Mockito service tests with Given/When/Then structure and strict stubbing.
    - [x] Add low-stock service tests.
    - [x] Add summary service tests.
  - [x] Verify exact lookup found/not-found, available quantity, pageable filter forwarding, low-stock query delegation, and summary delegation.
    - [x] Verify exact lookup found/not-found, available quantity, and pageable filter forwarding.
    - [x] Verify low-stock query delegation with and without a Warehouse filter.
    - [x] Verify summary delegation.
  - [x] Add controller tests for parameter binding, zero-based page metadata, envelopes, and documented response types.
    - [x] Add `GET /stock` controller tests for filter binding, zero-based page metadata, envelopes, validation, sort allow-list, and documented response types.
    - [x] Add low-stock controller tests for filter binding, page metadata, envelopes, validation, sort allow-list, authorization annotation, and documented response types.
    - [x] Add summary controller tests.
  - [x] Extend authorization integration coverage for unauthenticated and every currently supported role without weakening existing Product/Category/Warehouse checks.
    - [x] Extend integration coverage for unauthenticated and all current allowed roles on `GET /stock`, `GET /stock/low`, and `GET /stock/summary`.
    - [x] Record unsupported-role coverage as not applicable while `Role` contains only the three accepted authorities; add it if another authority is reintroduced.
  - [x] Correct the stale catalog-read integration-test expectation that treated `STAFF` as forbidden; the controllers and accepted matrix allow `STAFF`, `MANAGER`, and `ADMIN` to read.
- [x] Add PostgreSQL integration coverage (AC: 3-7, 9)
  - [x] Reuse the PostgreSQL Testcontainers approach established by the migration/search integration tests.
  - [x] Seed Products with different reorder values, Warehouses, and StockLevels that distinguish fixed-threshold behavior from product-specific low-stock behavior.
  - [x] Add test assertions for combined and independent filters, page boundaries, display joins, and `quantity - reserved_quantity`.
  - [x] Refresh `mv_stock_summary` only as test setup and add assertions for all four V7 statuses and mapped fields.
  - [x] Add a source-separation test proving transactional reads use `stock_levels` while summary reads retain the last materialized-view refresh.
  - [x] Record complete Docker-backed suite execution as deferred by the user at review handoff; production and test sources compile, and all 72 non-Docker tests pass.

## Dev Notes

### Scope and source-of-truth decisions

- This story implements REQ-STP-B-201 and REQ-STP-B-204 only.
- Story 2.1 already released V3 and V7. Those migrations are immutable and must not be edited.
- Story 2.3 owns the Redis cache and five-minute TTL. Do not add provisional caching here.
- Story 2.5 owns scheduled concurrent refresh and operational verification of `mv_stock_summary`.
- Story 3.2 owns pessimistic locking and stock mutation during movement completion.
- The User module remains part of the application and is outside this story.

### Persistence contract

`stock_levels` has:

| Java field | Database column | Notes |
|---|---|---|
| `id` | `id` | Generated `BIGSERIAL` identifier |
| `productId` | `product_id` | Required plain `Long` FK |
| `warehouseId` | `warehouse_id` | Required plain `Long` FK |
| `quantity` | `quantity` | Required, non-negative |
| `reservedQuantity` | `reserved_quantity` | Required, non-negative |
| `version` | `version` | Required `@Version` optimistic-lock field |
| `updatedAt` | `updated_at` | Required timestamp |

Do not inherit `BaseEntity`: its `createdAt` mapping would make `ddl-auto: validate` fail because V3 has no `created_at`.

### Read-model contract

Recommended `StockLevelResponse` fields:

- `id`
- `productId`, `productSku`, `productName`
- `warehouseId`, `warehouseCode`, `warehouseName`
- `quantity`, `reservedQuantity`, `availableQuantity`
- `reorderPoint`
- `version`, `updatedAt`

Recommended `StockSummaryResponse` fields mirror V7:

- `productId`, `sku`, `productName`, nullable `categoryName`
- `warehouseId`, `warehouseName`
- `quantity`, `reservedQuantity`, `availableQuantity`
- `minStock`, `reorderPoint`, `stockStatus`

The low-stock endpoint uses `quantity <= reorderPoint`, matching the graded requirement's stock-low rule. The summary status uses V7's separate min/max rules. Do not merge these two concepts.

### Query guidance

- Use JPQL or a native typed projection for transactional stock queries that join Product and Warehouse display fields while keeping entity foreign keys scalar.
- Optional filters should use explicit nullable predicates, for example `(:warehouseId IS NULL OR sl.warehouseId = :warehouseId)`.
- The summary repository query must name `mv_stock_summary` directly and supply a matching native `countQuery`.
- Allow-list public sort names and translate them when a native query uses snake_case columns. Reject unsupported sort properties with `BadRequestException` rather than surfacing a repository/runtime 500.
- Reads do not acquire pessimistic locks. `@Version` maps the schema for future writes but is not a display-query lock.

### API contract

All endpoints use the existing default page contract:

| Endpoint | Optional filters | Source |
|---|---|---|
| `GET /api/v1/stock` | `warehouseId`, `productId` | `stock_levels` joined to display tables |
| `GET /api/v1/stock/low` | `warehouseId` | `stock_levels` joined to `products` using each `reorder_point` |
| `GET /api/v1/stock/summary` | `warehouseId`, `productId` | `mv_stock_summary` only |

Default sorting should be deterministic. Prefer `updatedAt DESC, id DESC` for transactional stock and `productId ASC, warehouseId ASC` for the reporting view unless the existing public API contract requires another order.

### Testing guidance

- Unit tests mock repositories/projection mappers and verify service behavior without loading Spring.
- Web tests should use the existing `ApiResponse`/`PageResponse` shapes rather than asserting raw arrays.
- PostgreSQL behavior cannot be proven with H2. Docker Desktop must be running for the Testcontainers integration suite.
- `CatalogWarehouseAuthorizationIntegrationTest.everyRoleFollowsTheReadAuthorizationMatrix` currently expects `STAFF` to receive 403 even though the implemented controllers allow all three current roles to read. Correct this test expectation while adding Stock authorization coverage; do not restrict the production controllers to satisfy the stale assertion.
- A useful low-stock fixture has two Products with different reorder points and quantities that would produce the wrong result under a fixed threshold.
- A useful source-separation test refreshes the materialized view, changes a StockLevel in the base table, then proves `/stock` sees the current row while `/stock/summary` still reflects the last refresh.

### Expected affected files

- `backend/src/main/java/com/training/starter/entity/StockLevel.java` (new)
- `backend/src/main/java/com/training/starter/repository/StockLevelRepository.java` (new)
- `backend/src/main/java/com/training/starter/repository/StockSummaryRepository.java` or an equivalent read adapter (new)
- `backend/src/main/java/com/training/starter/dto/response/StockLevelResponse.java` (new)
- `backend/src/main/java/com/training/starter/dto/response/StockSummaryResponse.java` (new)
- `backend/src/main/java/com/training/starter/mapper/StockLevelMapper.java` or explicit projection mapping (new)
- `backend/src/main/java/com/training/starter/service/StockLevelService.java` (new)
- `backend/src/main/java/com/training/starter/service/impl/StockLevelServiceImpl.java` (new)
- `backend/src/main/java/com/training/starter/controller/StockController.java` (new)
- `backend/src/test/java/com/training/starter/service/StockLevelServiceTest.java` (new)
- Focused controller/security and PostgreSQL integration test files (new or updated)

Explicitly do not modify Flyway migration files, Redis/RabbitMQ configuration, movement code, frontend code, or the Weekly Requirements section.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 2, Story 2.2]
- [Source: `Project 4 - StockPulse.md` — REQ-STP-B-201 and REQ-STP-B-204]
- [Source: `AGENTS.md` — layering, entity, DTO, mapper, service, controller, Flyway, and testing rules]
- [Source: `backend/src/main/resources/db/migration/V3__create_stock_tables.sql` — transactional schema]
- [Source: `backend/src/main/resources/db/migration/V7__create_materialized_views.sql` — reporting projection and status rules]
- [Source: current Product/Warehouse vertical slices — pagination, response envelopes, authorization, logging, and test patterns]
- [Source: `_bmad-output/project-context.md` and `_bmad-output/project-documentation/backend-implementation-patterns.md`]

## Dev Agent Record

### Agent Model Used

OpenAI Codex (GPT-5)

### Debug Log References

- RED: `.\mvnw.cmd -Dtest=StockLevelRepositoryIntegrationTest test` failed compilation because `StockLevel` and `StockLevelRepository` did not exist.
- GREEN compile: `.\mvnw.cmd -DskipTests compile` passed with 65 production source files.
- Regression: `.\mvnw.cmd '-Dtest=*Test,!*IntegrationTest' test` passed 51 tests.
- PostgreSQL verification: the focused repository integration test compiles but cannot start because Docker Desktop is unavailable.
- Increment 2 RED: `.\mvnw.cmd -Dtest=StockLevelServiceTest test` failed compilation because the response, mapper, and service did not exist.
- Increment 2 GREEN: focused mapper/service tests passed 3 tests.
- Increment 2 regression: all 54 tests that do not require Docker passed.
- Increment 3 RED: `.\mvnw.cmd -Dtest=StockLevelServiceTest test` failed compilation because `StockLevelProjection` did not exist.
- Increment 3 GREEN: focused projection-mapper and service tests passed 6 tests.
- Increment 3 regression: all 57 tests that do not require Docker passed.
- Increment 4 GREEN: `StockControllerTest` passed 4 focused tests.
- Increment 4 OpenAPI: the first combined run exposed a stale `createdAt` default-sort assertion for every list; the stock-specific expectation was corrected to `updatedAt`, then all 6 OpenAPI tests passed.
- Increment 4 regression: all 61 tests that do not require Docker passed.
- Increment 5 RED: `.\mvnw.cmd -Dtest=StockLevelServiceTest test` failed compilation because the low-stock repository and service methods did not exist.
- Increment 5 GREEN: all 6 `StockLevelServiceTest` cases passed.
- Increment 5 regression: all 63 tests that do not require Docker passed.
- Increment 6 RED: the focused `StockControllerTest` cases for `/stock/low` returned 404 before the controller mapping existed.
- Increment 6 GREEN/OpenAPI: `StockControllerTest` and `Week1OpenApiDocumentationTest` passed all 13 focused tests.
- Increment 6 regression: all 66 tests that do not require Docker passed.
- Increment 7 RED: focused mapper/service tests failed compilation because the summary DTO, projection, repository, mapper, and service method did not exist.
- Increment 7 GREEN: `StockSummaryMapperTest` and `StockLevelServiceTest` passed all 9 focused tests.
- Increment 7 compile: all production and test sources, including the new PostgreSQL summary cases, compiled successfully.
- Increment 7 regression: all 69 tests that do not require Docker passed.
- Increment 7 PostgreSQL attempt: `StockLevelRepositoryIntegrationTest` could not start because Testcontainers found no valid Docker environment; no database assertion ran.
- Increment 8 RED: the three summary controller tests failed because `/stock/summary` and its controller method did not exist.
- Increment 8 GREEN/OpenAPI: `StockControllerTest` and `Week1OpenApiDocumentationTest` passed all 16 focused tests.
- Increment 8 regression: all 72 tests that do not require Docker passed.
- Review handoff: the user explicitly requested no further access to the WSL Docker daemon and accepted deferring Docker-backed runtime execution to review.

### Completion Notes List

- Increment 1 added the V3-aligned `StockLevel` mapping without `BaseEntity`, JPA relationships, or migration changes.
- Added the exact `(warehouseId, productId)` repository lookup needed by the future service and Story 2.3 cache boundary.
- Added PostgreSQL/Flyway integration coverage for a found pair, a missing pair, optimistic version initialization, and timestamp mapping.
- Runtime PostgreSQL verification remains pending until Docker Desktop is running; Story 2.2 remains in progress.
- Increment 2 added the enriched `StockLevelResponse`, a three-source MapStruct mapper, and an exact read-only service lookup.
- Missing stock fails before Product/Warehouse enrichment queries; unit coverage verifies both found and not-found paths.
- Mapper coverage verifies every flattened display field and derives `availableQuantity` as quantity minus reserved quantity.
- Increment 3 added a typed JPQL projection and one pageable repository query with nullable Warehouse/Product filters.
- The list query joins Product and Warehouse by scalar IDs, avoiding both `@ManyToOne` and service-layer N+1 enrichment.
- The service preserves Spring `Page` metadata while mapping projection rows to `StockLevelResponse`.
- PostgreSQL integration coverage now exercises display aliases, combined filters, derived availability, and zero-based page metadata; execution remains pending until Docker Desktop is available.
- Increment 4 added the authorized paginated `GET /api/v1/stock` endpoint with optional exact filters, explicit request validation, a safe sort allow-list, and the standard response envelope.
- OpenAPI documents the implemented Stock operations and concrete page schema while continuing to reject the future `/stock/summary` path.
- Authorization integration coverage includes the Stock read path and now correctly expects all current roles (`STAFF`, `MANAGER`, `ADMIN`) to read.
- Increment 5 added a pageable low-stock projection query and read-only service method with an optional Warehouse filter.
- Low-stock selection uses `quantity <= product.reorderPoint`; the fixed `quantity < 20` partial-index predicate is not used as a business rule.
- PostgreSQL integration coverage distinguishes rows that a fixed threshold would classify incorrectly and verifies the inclusive reorder-point boundary.
- Increment 6 exposed authorized `GET /api/v1/stock/low` with optional `warehouseId`, zero-based pagination, bounded page size, and the shared safe sort allow-list.
- Controller coverage verifies successful binding/envelopes and rejects invalid page, size, direction, and sort fields before invoking the service.
- OpenAPI and authorization integration coverage now include `/stock/low`; all current roles (`STAFF`, `MANAGER`, `ADMIN`) remain allowed to read.
- Increment 7 added a dedicated native `StockSummaryRepository` that reads only `mv_stock_summary`, with nullable exact filters and a matching native `countQuery`.
- `StockSummaryProjection`, `StockSummaryMapper`, and `StockSummaryResponse` keep reporting rows separate from transactional `StockLevel` reads.
- `StockStatus` fixes the response contract to the four V7 values: `OUT_OF_STOCK`, `LOW_STOCK`, `OVERSTOCK`, and `NORMAL`.
- The read-only service preserves page metadata and delegates summary mapping without consulting transactional repositories.
- PostgreSQL test cases cover every status, all projected fields, filters, page metadata, and stale-view source separation; execution remains pending until Docker Desktop is available.
- Increment 8 exposed authorized `GET /api/v1/stock/summary` with optional Warehouse/Product filters and the standard zero-based page envelope.
- The summary endpoint publishes only allow-listed response sort fields and translates each one to a fixed native materialized-view column before repository execution.
- Summary OpenAPI coverage documents `productId` as the default sort, every supported sort field, all response fields, and the standard 200/400/401/403 envelopes.
- Controller tests cover successful mapping, page metadata, native sort translation, invalid filters/pagination/sort, and the accepted role annotation.
- Story moved to `review` by explicit user direction. Docker-backed tests are implemented and compile, but their runtime assertions were not executed in this session; reviewers must not interpret the checked coverage tasks as a passing Docker test run.

### File List

- `backend/src/main/java/com/training/starter/entity/StockLevel.java` (new)
- `backend/src/main/java/com/training/starter/repository/StockLevelRepository.java` (new)
- `backend/src/main/java/com/training/starter/repository/projection/StockLevelProjection.java` (new)
- `backend/src/main/java/com/training/starter/repository/StockSummaryRepository.java` (new)
- `backend/src/main/java/com/training/starter/repository/projection/StockSummaryProjection.java` (new)
- `backend/src/main/java/com/training/starter/dto/response/StockLevelResponse.java` (new)
- `backend/src/main/java/com/training/starter/dto/response/StockSummaryResponse.java` (new)
- `backend/src/main/java/com/training/starter/enums/StockStatus.java` (new)
- `backend/src/main/java/com/training/starter/mapper/StockLevelMapper.java` (new)
- `backend/src/main/java/com/training/starter/mapper/StockSummaryMapper.java` (new)
- `backend/src/main/java/com/training/starter/service/StockLevelService.java` (new)
- `backend/src/main/java/com/training/starter/service/impl/StockLevelServiceImpl.java` (new)
- `backend/src/main/java/com/training/starter/controller/StockController.java` (new)
- `backend/src/main/java/com/training/starter/config/OpenApiSchemas.java` (updated)
- `backend/src/test/java/com/training/starter/repository/StockLevelRepositoryIntegrationTest.java` (new)
- `backend/src/test/java/com/training/starter/mapper/StockLevelMapperTest.java` (new)
- `backend/src/test/java/com/training/starter/mapper/StockSummaryMapperTest.java` (new)
- `backend/src/test/java/com/training/starter/service/StockLevelServiceTest.java` (new)
- `backend/src/test/java/com/training/starter/controller/StockControllerTest.java` (new)
- `backend/src/test/java/com/training/starter/controller/Week1OpenApiDocumentationTest.java` (updated)
- `backend/src/test/java/com/training/starter/security/CatalogWarehouseAuthorizationIntegrationTest.java` (updated)
- `_bmad-output/implementation-artifacts/2-2-expose-stock-levels-low-stock-and-summary-apis.md` (updated)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (updated)
