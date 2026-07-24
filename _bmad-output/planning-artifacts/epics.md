# StockPulse Implementation Epics and Stories

**Authoritative inputs:** `Project 4 - StockPulse.md` v1.1, `_bmad-output/project-context.md`, and `_bmad-output/project-documentation/`  
**Baseline date:** 2026-07-21, after the Warehouse and Category CRUD merge  
**Scope rule:** This is a brownfield delta plan. Implemented behavior is not planned again; partially implemented requirements include only their documented missing work.

## Planning Decisions

- Existing Warehouse and Category entities, migrations, DTOs, mappers, repositories, services, controllers, and unit tests are accepted baseline dependencies. No story recreates them.
- Existing Product CRUD, Product Angular list/form flow, authentication, User CRUD, V1, and V2 are modified only where a requirement remains incomplete.
- Backend additions follow controller → service interface → transactional service implementation → repository, use record DTOs with Bean Validation, MapStruct, `ApiResponse<T>`/`PageResponse<T>`, global exception handling, and Flyway-owned schema.
- Angular additions use standalone lazy-loaded components, typed feature services, strict TypeScript, reactive forms, Angular Material, existing notifications, and zero-based server pagination.
- Only movement completion changes stock. It performs the stock mutation synchronously in one database transaction. The required `StockUpdateConsumer` evaluates the already-committed result and must not apply the movement a second time.
- Event publication occurs only after a successful movement commit. Consumers use a stable event ID for idempotency. This meets the stated scope without introducing an outbox subsystem not requested by the product requirements.
- Role authorization is enforced by the backend. `ADMIN` can perform all StockPulse operations, `MANAGER` can also perform `STAFF` operations, and `STAFF` receives the read/create/complete permissions listed in the endpoint matrix. Existing `USER` remains for starter compatibility but receives no implicit StockPulse domain permissions.

## Shared Definition of Done

Every story must preserve the established project patterns, compile cleanly, keep JPA mappings aligned with Flyway, expose no entities from controllers, and add OpenAPI detail for any changed endpoint. Tests use JUnit 5/Mockito/AssertJ with Given/When/Then for units, `BaseIntegrationTest` plus real infrastructure for integration behavior, and Jasmine/Karma for Angular behavior. Changed backend tests and affected frontend build/tests must pass.

---

## Epic 1 — Complete Catalog and Location Workflows

**Business outcome:** Staff and managers can use the existing product, category, and warehouse capabilities safely through complete UI flows and correct authorization.

### Story 1.1 — Enforce the catalog and warehouse authorization matrix

**Requirement IDs:** REQ-STP-B-106  
**Business value:** Prevents authenticated users from performing inventory administration outside their responsibilities while retaining the already-implemented paginated APIs.

**Acceptance criteria**

1. Add `STAFF` and `MANAGER` to the string-backed role model and enable method authorization; keep existing `ADMIN` and `USER` values readable.
2. Product/category list and detail allow `STAFF`, `MANAGER`, and `ADMIN`; create/update allow `MANAGER` and `ADMIN`.
3. Warehouse list/detail allow `STAFF`, `MANAGER`, and `ADMIN`; create/update allow only `ADMIN`.
4. Unauthenticated calls return 401 through the existing security flow; authenticated but unauthorized calls return the existing 403 error envelope.
5. Do not change the implemented endpoint paths, pagination envelopes, immutable keys, Category hierarchy rules, or Warehouse soft-deactivation behavior.

**Dependencies:** Existing JWT security, Product/Warehouse/Category controllers and role storage.  
**Impact:** Backend—role enum, security configuration, controller/service authorization; Frontend—none in this story; Database—no migration because roles are stored as strings.  
**Required tests:** Authorization integration tests for unauthenticated, `USER`, `STAFF`, `MANAGER`, and `ADMIN` across representative read/write endpoints; existing service tests remain unchanged.  
**Concurrency/transaction/cache/messaging:** Existing service transaction boundaries remain authoritative; no cache or messaging impact.

### Story 1.2 — Activate PostgreSQL product full-text search

**Requirement IDs:** REQ-STP-B-105  
**Business value:** Gives staff scalable SKU/name discovery while retaining the already-implemented Product CRUD service.

**Acceptance criteria**

1. Populate `products.search_vector` from SKU and name for existing rows and on future insert/update using PostgreSQL-supported SQL in an immutable Flyway migration.
2. Add the documented GIN index and change only the existing search path to call `ProductRepository.searchByVector`; do not recreate Product CRUD.
3. Normalize/escape user input safely and return the existing paginated `ProductSummaryResponse` contract.
4. Blank search text is rejected with the established 400 envelope; searches are case-insensitive and match SKU or name terms.

**Dependencies:** Story 2.1 owns V6 migration delivery; existing Product repository/service/controller.  
**Impact:** Backend—Product search repository/service path; Frontend—existing Product search remains contract-compatible; Database—search-vector backfill/trigger and GIN index in V6.  
**Required tests:** Unit tests only for the changed service delegation/validation; Flyway-enabled PostgreSQL integration tests prove backfill, insert/update vector maintenance, matching, no-match, and pagination. Existing REQ-STP-T-101 tests are not recreated.  
**Concurrency/transaction/cache/messaging:** Trigger updates in the Product write transaction; no cache or messaging impact.

### Story 1.3 — Complete required Product list columns

**Requirement IDs:** REQ-STP-F-101  
**Business value:** Lets staff see replenishment thresholds without opening each already-implemented Product detail/edit flow.

**Acceptance criteria**

1. Add `minStock` and `reorderPoint` to the existing Product list model/response and render Min Stock and Reorder Point columns in the existing paginated Material table.
2. Preserve current SKU, name, category, unit, status, actions, loading, empty, error, and zero-based pagination behavior.
3. Keep the API wrapped in `ApiResponse<PageResponse<ProductSummaryResponse>>`; do not build a replacement Product list.

**Dependencies:** Existing Product list endpoint, `ProductListComponent`, and Product feature service.  
**Impact:** Backend—extend Product summary mapping only if fields are absent; Frontend—existing model/table; Database—none.  
**Required tests:** Mapper/service unit assertion for the two response fields if backend changes; Angular component test verifies both columns, values, pagination, and error state.  
**Concurrency/transaction/cache/messaging:** Not applicable.

### Story 1.4 — Replace Product category ID entry with a dropdown

**Requirement IDs:** REQ-STP-F-102  
**Business value:** Prevents invalid category entry and makes the existing Product create/edit form usable by business users.

**Acceptance criteria**

1. Replace only the numeric category input with an Angular Material select populated from the existing paginated Category API through a typed feature service.
2. Store/send the selected category ID in the existing Product request contract; preserve all current Product form fields, validation, create/edit branching, and disabled SKU on edit.
3. Provide loading, empty, and API-error states for category options and prevent submission while required reference data is unavailable.
4. Editing an existing product preselects its category, including categories loaded from a later server page; the implementation must not silently assume all categories fit on one default page.

**Dependencies:** Existing Product form and implemented Category list API; Story 1.1 authorization.  
**Impact:** Backend—none; Frontend—Product form, typed Category model/service reuse or addition; Database—none.  
**Required tests:** Angular service test for envelope parsing; component tests for option loading, preselection, validation, payload ID, empty/error states, and create/edit behavior.  
**Concurrency/transaction/cache/messaging:** Not applicable.

### Story 1.5 — Add the Warehouse list route and navigation

**Requirement IDs:** REQ-STP-F-103, REQ-STP-F-104  
**Business value:** Makes the merged Warehouse backend available to authorized users without direct API calls.

**Acceptance criteria**

1. Add a standalone `WarehouseListComponent` and typed service under `features/warehouses` using the existing paginated Warehouse API; do not recreate backend Warehouse behavior.
2. The table shows Name, Code, Address, and Status, with loading, empty, error, and server-paginated states using 10/20/50 page sizes.
3. Add a lazy protected Warehouse route under `MainLayoutComponent` and a sidebar link visible to authenticated StockPulse roles.
4. Because the requirement asks for a list only, do not add Warehouse create/edit forms beyond the stated scope.

**Dependencies:** Implemented Warehouse API; Story 1.1 authorization.  
**Impact:** Backend—none; Frontend—Warehouse model/service/list, routes, sidebar; Database—none.  
**Required tests:** Angular service pagination/error tests; component loading/empty/content/error/paginator tests; route smoke test. Existing REQ-STP-T-102 backend tests are not recreated.  
**Concurrency/transaction/cache/messaging:** Not applicable.

---

## Epic 2 — Provide Trustworthy Stock Visibility

**Business outcome:** Staff can see accurate stock, shortages, and reporting summaries with safe caching and a consistent database foundation.

### Story 2.1 — Add the remaining StockPulse schema migrations

**Requirement IDs:** REQ-STP-B-107  
**Business value:** Establishes durable stock, movement, alert, reorder, index, and reporting structures required by every later inventory workflow.

**Acceptance criteria**

1. Preserve V1/V2 unchanged and add V3 `stock_levels`, V4 `stock_movements`/items, V5 alerts/reorder suggestions, V6 indexes/search-vector support, and V7 `mv_stock_summary` plus its unique index exactly as specified.
2. JPA enums use string values matching SQL; uniqueness, foreign keys, non-negative checks, positive movement-item quantity, timestamps, and optimistic `version` align between SQL and entities.
3. V6 contains the Product search-vector backfill/maintenance required by Story 1.2 in addition to the specified indexes.
4. A clean PostgreSQL 16 database migrates from V1 through V7; a database already at V2 migrates without data loss; Flyway validation passes on restart.

**Dependencies:** Existing immutable V1/V2 and `BaseEntity`.  
**Impact:** Backend—future entities must match schema; Frontend—none; Database—V3–V7 only.  
**Required tests:** Dedicated Flyway-enabled PostgreSQL integration test validates clean and V2-upgrade paths, constraints, indexes, search trigger, materialized view, and repeatable seed compatibility.  
**Concurrency/transaction/cache/messaging:** Stock unique/version columns support locking; the unique materialized-view index enables concurrent refresh.

### Story 2.2 — Expose stock levels, low stock, and summary APIs

**Requirement IDs:** REQ-STP-B-201, REQ-STP-B-204  
**Business value:** Gives staff a transactional source of truth for inventory and a reporting view for operational decisions.

**Acceptance criteria**

1. Implement StockLevel entity/repository/service using the established layers and plain `productId`/`warehouseId` IDs; available quantity is `quantity - reservedQuantity`.
2. `GET /api/v1/stock` returns a zero-based paginated, filterable stock list; `GET /stock/low` uses each product's reorder configuration; `GET /stock/summary` reads `mv_stock_summary` only.
3. Responses include product/warehouse display data needed by REQ-STP-F-201 and use `ApiResponse<PageResponse<...>>` where paged.
4. Summary statuses use the specified zero/min/max rules; the materialized view is never used as the transactional source for movement validation.
5. All endpoints allow `STAFF`, `MANAGER`, and `ADMIN` and reject `USER`.

**Dependencies:** Stories 1.1 and 2.1.  
**Impact:** Backend—stock entity/repository/service/controller/records/mapper; Frontend—API contracts for Stories 2.4 and 5.1; Database—queries against V3/V7.  
**Required tests:** Service units for found/not-found and product-specific low-stock logic; PostgreSQL integration tests for list filters, pagination, available quantity, thresholds, and summary mapping; authorization tests.  
**Concurrency/transaction/cache/messaging:** Read-only transactions; do not lock for display queries; cache behavior is isolated to Story 2.3.

### Story 2.3 — Cache individual stock lookups and invalidate after commit

**Requirement IDs:** REQ-STP-B-206, REQ-STP-T-202  
**Business value:** Speeds frequent availability reads without returning stale stock after movements.

**Acceptance criteria**

1. Cache individual product/warehouse stock responses at `stock:{warehouseId}:{productId}` with a five-minute TTL using existing Redis configuration and typed serialization.
2. A cache miss queries PostgreSQL and populates Redis; a hit avoids the repository.
3. Movement completion invalidates all affected keys only after the database transaction commits; rollback leaves prior valid cache entries untouched.
4. Missing stock is not cached as a durable false result.

**Dependencies:** Story 2.2; Story 3.2 supplies completion integration.  
**Impact:** Backend—stock service/cache adapter and after-commit invalidation; Frontend—none; Database—none.  
**Required tests:** Mockito units for miss/hit; Redis integration tests verify key format, serialized value, TTL near five minutes, multi-item invalidation after commit, and no invalidation on rollback.  
**Concurrency/transaction/cache/messaging:** Cache is an optimization, never the completion authority; commit-ordered eviction prevents exposure of rolled-back data.

### Story 2.4 — Add the stock-level screen

**Requirement IDs:** REQ-STP-F-201  
**Business value:** Lets staff scan availability and shortage status by product and warehouse.

**Acceptance criteria**

1. Add a standalone stock list using a typed service and render Product, Warehouse, Quantity, Reserved, Available, and Status.
2. Use server pagination and API-supported product/warehouse filters; show loading, empty, error, and content states.
3. Show `OUT_OF_STOCK` in red, `LOW_STOCK` in orange, `OVERSTOCK` in yellow, and `NORMAL` distinctly; every state includes text and accessible semantics, not color alone.
4. Lazy-load the protected route and preserve zero-based page indices.

**Dependencies:** Story 2.2 and authentication baseline.  
**Impact:** Backend—none beyond Story 2.2 contract; Frontend—stock models/service/component/route/navigation; Database—none.  
**Required tests:** Angular service envelope/filter tests and component tests for all status styles/text, pagination, filters, loading, empty, and error states.  
**Concurrency/transaction/cache/messaging:** UI treats server data as a snapshot and refreshes on user action/page/filter changes.

### Story 2.5 — Refresh and verify the stock reporting view

**Requirement IDs:** REQ-STP-B-303, REQ-STP-B-306  
**Business value:** Keeps stock summaries current while proving reporting queries use the intended PostgreSQL indexes.

**Acceptance criteria**

1. Schedule `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_summary` with `fixedDelay = 60000`; execute outside an incompatible surrounding transaction.
2. Prevent overlapping refreshes within one application instance; log failure and duration and allow the next scheduled attempt.
3. Capture reproducible `EXPLAIN ANALYZE` evidence for stock list, low-stock, movement filter, and materialized-summary queries at representative seeded volume.
4. Evidence identifies the intended V6/V7 index for each query or records a justified query/index correction within stated requirements.

**Dependencies:** Stories 2.1 and 2.2.  
**Impact:** Backend—scheduler/configuration and query tuning; Frontend—none; Database—concurrent view refresh and documented plans.  
**Required tests:** PostgreSQL integration test changes stock, refreshes, and verifies summary; scheduler service unit covers success/failure/non-overlap; checked-in performance evidence or test fixture reproduces plans.  
**Concurrency/transaction/cache/messaging:** Unique view index is mandatory; reporting refresh must not lock transactional stock reads/writes unnecessarily.

---

## Epic 3 — Execute Controlled Stock Movements

**Business outcome:** Staff can import, export, and transfer stock through an approved, atomic, idempotent lifecycle that never makes inventory negative.

### Story 3.1 — Create and inspect stock movements

**Requirement IDs:** REQ-STP-B-202  
**Business value:** Records intended inventory changes and their line items before stock is changed.

**Acceptance criteria**

1. Implement movement/item entities, record DTOs, MapStruct mapping, repositories, service interface/implementation, and the specified `DRAFT → PENDING_APPROVAL → APPROVED → COMPLETED/REJECTED` plus cancellation rules.
2. `createImport`, `createExport`, and `createTransfer` persist a unique reference and all items atomically; quantity is positive and product/warehouse references must exist.
3. Transfers require distinct source/destination warehouses; duplicate product lines in one movement are rejected rather than ambiguously combined.
4. Creating or inspecting a movement never changes stock; store `createdBy` from the authenticated principal.

**Dependencies:** Story 2.1 and existing Product/Warehouse baseline.  
**Impact:** Backend—movement domain layers; Frontend—contracts for Stories 3.4/3.5; Database—V4.  
**Required tests:** Service units for all three creation types, invalid references, duplicate lines, invalid quantities, transfer warehouse rule, unique reference conflict, status initialization, and atomic failure.  
**Concurrency/transaction/cache/messaging:** Create is one transaction; no cache eviction or event publication before completion.

### Story 3.2 — Approve and complete movements with ordered locking

**Requirement IDs:** REQ-STP-B-203, REQ-STP-T-201  
**Business value:** Applies authorized stock changes safely, including under concurrent exports.

**Acceptance criteria**

1. Only `MANAGER`/`ADMIN` approves; approval records `approvedBy`. Only an approved movement can complete, and invalid transitions return 409 without side effects.
2. In one transaction, lock all affected `stock_levels` pessimistically in ascending product-ID order before validation or mutation.
3. Import adds to the selected warehouse; export subtracts only when available stock is sufficient; transfer subtracts source and adds destination atomically.
4. Use batched persistence with the configured batch size/order. Any invalid item rolls back every item and leaves caches/events untouched.
5. Completion is idempotent: a completed movement cannot mutate stock or publish a second completion event.

**Dependencies:** Stories 2.2 and 3.1.  
**Impact:** Backend—movement/stock repositories and transactional service; Frontend—action contract; Database—pessimistic row locks and version updates.  
**Required tests:** At minimum the four REQ-STP-T-201 units: create import, complete import increase, complete export decrease, insufficient export; additionally transfer, invalid states, duplicate completion, deterministic lock order, batch rollback, and authorization.  
**Concurrency/transaction/cache/messaging:** Stock mutation is synchronous and authoritative; invalidate cache and publish only after commit via Stories 2.3/4.1.

### Story 3.3 — Expose movement lifecycle endpoints

**Requirement IDs:** REQ-STP-B-205  
**Business value:** Makes import, export, transfer, review, approval, and completion available to clients through stable contracts.

**Acceptance criteria**

1. Implement the exact endpoints in the requirements: three create endpoints, paginated list, detail, approve, and complete under `/api/v1/movements`.
2. Create/list/detail/complete allow `STAFF`, `MANAGER`, and `ADMIN`; approve allows `MANAGER` and `ADMIN`.
3. List supports type/status filters required by REQ-STP-F-203 with validated enum input and zero-based pagination.
4. Controllers contain HTTP concerns only, validate record DTOs, return standard envelopes/status codes, and delegate state/transaction rules to services.

**Dependencies:** Stories 1.1, 3.1, and 3.2.  
**Impact:** Backend—controller/DTO/OpenAPI mapping; Frontend—movement feature contracts; Database—none beyond movement services.  
**Required tests:** Controller/integration tests for every endpoint, filter/pagination behavior, validation, 404/409, and role matrix.  
**Concurrency/transaction/cache/messaging:** Endpoint retries cannot double-complete; service transaction remains the boundary.

### Story 3.4 — Create movement forms and movement history

**Requirement IDs:** REQ-STP-F-202, REQ-STP-F-203  
**Business value:** Lets staff record imports/exports and find operational history without calling APIs manually.

**Acceptance criteria**

1. Add a standalone import/export reactive form with warehouse selection and dynamic item rows containing product, positive quantity, and unit cost; support add/remove while requiring at least one item.
2. Load products/warehouses through typed services without assuming a single default API page; submit the exact typed import/export request.
3. Add a paginated movement list showing Reference, Type, Status, Warehouse, and Date with server-side type/status filters.
4. Both screens implement loading/submitting, empty, validation, API-error, success-notification, and navigation behavior using existing patterns.

**Dependencies:** Stories 3.3, 1.1, and existing Product/Warehouse read APIs.  
**Impact:** Backend—none; Frontend—movement models/service/form/list/routes/navigation; Database—none.  
**Required tests:** Angular service request/filter tests; form tests for dynamic rows, validation, payload, success/error; list tests for filter reset, pagination, empty/error/content.  
**Concurrency/transaction/cache/messaging:** Disable duplicate submission while pending; backend remains responsible for uniqueness and transactions.

### Story 3.5 — Review, approve, and complete movement details

**Requirement IDs:** REQ-STP-F-204  
**Business value:** Gives authorized users a clear, state-aware control point for movement execution.

**Acceptance criteria**

1. Show movement header, source/destination, notes, audit users/timestamps, and complete item list.
2. Approve is visible only to `MANAGER`/`ADMIN` in an approvable state; Complete is visible to `STAFF`/`MANAGER`/`ADMIN` only when approved.
3. UI visibility is convenience only; 403/409 responses are handled explicitly and the detail is refreshed after every action.
4. Prevent repeated clicks while an action is pending and never optimistically display completed stock before the API succeeds.

**Dependencies:** Stories 1.1 and 3.3; existing role in `AuthService`.  
**Impact:** Backend—none; Frontend—detail component, role/state action logic, route; Database—none.  
**Required tests:** Angular tests for each role/state combination, action calls, pending guard, success refresh, and 403/409 error handling.  
**Concurrency/transaction/cache/messaging:** UI tolerates stale status; backend idempotency/conflict rules decide concurrent action outcomes.

### Story 3.6 — Prove the movement lifecycle and concurrent export safety

**Requirement IDs:** REQ-STP-B-301, REQ-STP-B-302, REQ-STP-T-302  
**Business value:** Demonstrates that the core inventory lifecycle works against real PostgreSQL and cannot oversell under contention.

**Acceptance criteria**

1. A `BaseIntegrationTest` flow creates an import, approves it, completes it, and verifies movement state plus persisted stock increase.
2. A concurrency test creates five independently transacted exports for the same product/warehouse, starts completion concurrently, and waits for all outcomes.
3. The initial stock permits fewer than all exports; only feasible completions succeed, failed operations roll back, and persisted quantity/available quantity never becomes negative.
4. The test uses real PostgreSQL locks, not mocks or one shared transaction, and has deterministic timeouts/failure diagnostics.

**Dependencies:** Stories 3.2 and 3.3.  
**Impact:** Backend—test harness only unless defects found; Frontend—none; Database—real transactional test data.  
**Required tests:** The integration and concurrency tests are the deliverable; retain unit coverage from Story 3.2.  
**Concurrency/transaction/cache/messaging:** Explicitly validates ordered pessimistic locking, independent transactions, rollback, idempotency, and non-negative stock.

---

## Epic 4 — Automate Low-Stock Response

**Business outcome:** Completed movements reliably create actionable alerts, reorder suggestions, and email notifications, with retry and dead-letter recovery.

### Story 4.1 — Configure StockPulse events, retry, and dead-letter routing

**Requirement IDs:** REQ-STP-B-207, REQ-STP-B-305  
**Business value:** Decouples post-movement work and prevents transient consumer failures from silently losing operational actions.

**Acceptance criteria**

1. Add durable topic exchange `stock.exchange`, durable stock-update/reorder/email/audit queues, and exact routing patterns from the requirements; do not repurpose the generic `training.*` topology.
2. Publish one `StockMovementCompletedEvent` after successful transaction commit with stable event ID, movement ID/reference/type, affected product/warehouse IDs, and timestamp.
3. Configure bounded retry and a dead-letter exchange/queue for every StockPulse queue; preserve event/correlation IDs and failure metadata.
4. JSON contracts are versionable records and consumers reject malformed messages to DLQ rather than loop forever.

**Dependencies:** Story 3.2 and existing RabbitMQ configuration.  
**Impact:** Backend—Rabbit configuration, event records, publisher, after-commit hook; Frontend—none; Database—no new tables beyond V4/V5.  
**Required tests:** Configuration/context test for bindings/routing; publisher integration test proves after-commit delivery and no delivery on rollback; JSON round-trip test.  
**Concurrency/transaction/cache/messaging:** At-least-once delivery is assumed; stable IDs and consumer idempotency prevent duplicate effects.

### Story 4.2 — Create alerts and reorder suggestions from committed stock

**Requirement IDs:** REQ-STP-B-208, REQ-STP-B-209, REQ-STP-B-304  
**Business value:** Turns low inventory into a durable, manager-actionable replenishment workflow.

**Acceptance criteria**

1. `StockUpdateConsumer` receives completed events, reads committed stock, and evaluates each affected product/warehouse against its product-specific reorder point; it must not update stock again.
2. At/below reorder point, create one active `StockAlert` for the unresolved condition and publish one `StockLowEvent`; zero stock uses `OUT_OF_STOCK`, otherwise `LOW_STOCK`.
3. `ReorderConsumer` creates one pending suggestion using product `reorderQuantity`, current stock, and reorder point. Redelivery cannot duplicate an unresolved alert or pending suggestion.
4. Expose required alert list/acknowledge and reorder list/approve endpoints with `STAFF` and `MANAGER` authorization exactly as specified; approval/acknowledgement records state/timestamps.
5. When stock no longer qualifies, no new low event is emitted; existing record lifecycle remains explicit rather than being silently deleted.

**Dependencies:** Stories 2.1, 3.2, and 4.1.  
**Impact:** Backend—alert/reorder entities and full layers, consumers/controllers; Frontend—contracts for Story 4.4; Database—V5 uniqueness/query support.  
**Required tests:** Units for threshold boundary, out-of-stock, normal stock, duplicate unresolved condition, reorder quantity, acknowledgement/approval and authorization; integration test covers event → alert → suggestion against PostgreSQL/RabbitMQ.  
**Concurrency/transaction/cache/messaging:** Consumer operations are transactional and idempotent; database uniqueness/locking closes races between duplicate deliveries.

### Story 4.3 — Send low-stock email through MailHog

**Requirement IDs:** REQ-STP-B-209  
**Business value:** Notifies staff of shortages outside the application so urgent replenishment is not missed.

**Acceptance criteria**

1. `EmailConsumer` handles the low-stock routing pattern and sends an email containing product, warehouse, current stock, threshold, suggested quantity, and severity through configured SMTP/MailHog.
2. Use environment-backed mail configuration already present; do not hard-code recipients, hosts, or credentials.
3. A stable event ID prevents duplicate successful notifications from redelivery; failed sends throw for configured retry and ultimately reach the email DLQ.

**Dependencies:** Stories 4.1 and 4.2; MailHog from Docker Compose.  
**Impact:** Backend—email consumer/template/idempotency handling; Frontend—none; Database—reuse event/domain record identifiers only.  
**Required tests:** Unit verifies message content and duplicate suppression; integration test verifies MailHog delivery and a forced SMTP failure follows retry/DLQ behavior.  
**Concurrency/transaction/cache/messaging:** Email is post-commit asynchronous work; duplicate deliveries must not send duplicate successful messages.

### Story 4.4 — Add alert and reorder action screens

**Requirement IDs:** REQ-STP-F-301, REQ-STP-F-302  
**Business value:** Lets staff acknowledge shortages and managers approve replenishment suggestions from the application.

**Acceptance criteria**

1. Alert list shows active alerts with product, warehouse, quantities, threshold, and severity; red/orange/yellow semantics match the brief and include accessible text.
2. Staff, Manager, and Admin can acknowledge; the action updates the row only after success and handles stale/conflict responses.
3. Reorder list shows pending suggestions with product information, current stock, reorder point, and suggested quantity; approve appears only to Manager/Admin.
4. Use typed services, standard envelopes, server pagination where exposed, and loading/empty/error/success states.

**Dependencies:** Stories 1.1 and 4.2.  
**Impact:** Backend—none; Frontend—alert/reorder models, services, lists, routes/navigation; Database—none.  
**Required tests:** Angular service tests and component tests for severity accessibility, role visibility, pagination/states, action success, 403, and 409.  
**Concurrency/transaction/cache/messaging:** Backend state transition wins on concurrent acknowledge/approve; UI refreshes after conflict.

### Story 4.5 — Verify retry-to-DLQ and idempotent redelivery

**Requirement IDs:** REQ-STP-T-303  
**Business value:** Proves failed asynchronous work is recoverable and does not corrupt inventory workflows.

**Acceptance criteria**

1. Force each representative consumer failure, verify the configured retry count, then verify delivery to the correct DLQ with event/correlation/failure metadata.
2. Confirm no partial alert, suggestion, or notification success marker commits when a consumer transaction fails.
3. Redrive a corrected message and verify exactly one domain result; redeliver the successful event again and verify no duplicate.

**Dependencies:** Stories 4.1–4.3.  
**Impact:** Backend—messaging integration tests and test hooks only; Frontend—none; Database—assert transactional results.  
**Required tests:** RabbitMQ Testcontainers retry/DLQ/redrive/idempotency integration suite.  
**Concurrency/transaction/cache/messaging:** This story explicitly verifies at-least-once delivery, transactional rollback, bounded retry, DLQ, and idempotency.

---

## Epic 5 — Monitor and Validate Inventory Operations

**Business outcome:** Stakeholders can see operational health, trust API documentation, and verify the complete StockPulse business journey.

### Story 5.1 — Replace the starter dashboard with stock metrics

**Requirement IDs:** REQ-STP-F-303  
**Business value:** Gives staff an immediate view of catalog size, shortages, and pending movement workload.

**Acceptance criteria**

1. Show Total Products, Low Stock, Out of Stock, and Pending Movements using authoritative existing Product/Stock/Movement APIs and server-reported totals; do not add unrelated analytics.
2. Count definitions match the stock status rules and movement statuses used elsewhere.
3. Each card has loading/error/empty behavior and navigates to its corresponding filtered list where that list exists.

**Dependencies:** Stories 2.2, 3.3, and current Product API.  
**Impact:** Backend—only add minimal count/filter support to specified list endpoints if their existing page metadata cannot provide a required total; Frontend—Dashboard component/service composition; Database—existing indexed queries.  
**Required tests:** Backend query test for any added count/filter; Angular tests for all cards, partial API failure, totals, and navigation.  
**Concurrency/transaction/cache/messaging:** Dashboard is a point-in-time view; no cross-query snapshot transaction is required.

### Story 5.2 — Complete and verify StockPulse OpenAPI documentation

**Requirement IDs:** REQ-STP-B-307, REQ-STP-T-103  
**Business value:** Makes the delivered APIs understandable and verifiable for reviewers and client developers.

**Acceptance criteria**

1. Retain existing basic annotations and add complete annotations only where missing: StockPulse endpoints, request constraints, roles/security, pagination/filter parameters, success envelopes, and 400/401/403/404/409 responses.
2. Swagger UI exposes every endpoint required by the product specification with the correct method/path and usable schemas; no unimplemented endpoint is documented as available.
3. Record a repeatable verification checklist or automated OpenAPI assertion mapping every specified endpoint to the generated document.

**Dependencies:** All endpoint stories in Epics 1–4.  
**Impact:** Backend—annotations/config and verification test; Frontend—none; Database—none.  
**Required tests:** Spring context/OpenAPI JSON assertions for required paths, operations, security, and schemas; documented Swagger UI smoke verification.  
**Concurrency/transaction/cache/messaging:** Not applicable.

### Story 5.3 — Build the required Testcontainers integration suite

**Requirement IDs:** REQ-STP-T-301  
**Business value:** Protects core behavior across real infrastructure boundaries rather than relying only on mocks.

**Acceptance criteria**

1. Add at least five executable tests extending `BaseIntegrationTest` that cover Product CRUD, movement lifecycle, stock-level queries, alert creation, and one critical rollback/failure path.
2. Use PostgreSQL/Redis/RabbitMQ containers already configured; use a separate Flyway-enabled test where PostgreSQL-specific V3–V7 objects are required because the current test profile disables Flyway.
3. Tests isolate data, authenticate through supported test setup, avoid order dependence, and assert both API envelopes and persisted/infrastructure side effects.

**Dependencies:** Relevant implementation stories in Epics 1–4.  
**Impact:** Backend—test sources/config only; Frontend—none; Database—container fixtures.  
**Required tests:** The five-or-more integration cases are the deliverable and complement, not duplicate, story-level units/concurrency/messaging tests.  
**Concurrency/transaction/cache/messaging:** Include committed side-effect assertions and avoid hiding behavior inside a test-wide rollback.

### Story 5.4 — Prove the end-to-end StockPulse journey

**Requirement IDs:** REQ-STP-W-401  
**Business value:** Demonstrates the complete product outcome from catalog creation through replenishment notification.

**Acceptance criteria**

1. Through supported APIs/UI, create a product, import/approve/complete stock, verify stock increases, export/approve/complete to the reorder threshold, and verify stock decreases.
2. Verify exactly one low-stock alert, reorder suggestion, and MailHog email result; then acknowledge the alert and approve the suggestion with authorized users.
3. Capture deterministic evidence with product SKU, movement references, event IDs, final stock, and resulting record IDs; rerunning with isolated data succeeds.
4. The flow uses implemented security and asynchronous messaging rather than direct database shortcuts.

**Dependencies:** Epics 1–4 and Story 5.3.  
**Impact:** Backend—end-to-end test/fixtures only unless defects found; Frontend—optional scripted/manual evidence for UI steps; Database—isolated test data.  
**Required tests:** One automated or repeatably scripted end-to-end flow plus a documented UI smoke pass for the same journey.  
**Concurrency/transaction/cache/messaging:** Wait with bounded polling for asynchronous results; assert after-commit publication, cache freshness, idempotency, and no negative stock.

---

## Epic 6 — Prepare StockPulse for Review and Demonstration

**Business outcome:** The team supplies the review, defect, performance, documentation, and demonstration evidence required for completion.

### Story 6.1 — Complete the Team 1 cross-review

**Requirement IDs:** REQ-STP-W-402  
**Business value:** Improves both teams through concrete peer feedback and satisfies the training collaboration outcome.

**Acceptance criteria**

1. Review Team 1 OrderFlow code and submit at least ten distinct, actionable comments tied to exact files/lines or tests.
2. Comments address correctness, security, maintainability, testing, or performance and avoid duplicates/style-only padding.
3. Record links or exported evidence in the agreed project documentation location.

**Dependencies:** Access to Team 1 repository/review channel.  
**Impact:** Backend—none; Frontend—none; Database—none.  
**Required tests:** No code tests; verify evidence count and link accessibility.  
**Concurrency/transaction/cache/messaging:** Not applicable.

### Story 6.2 — Resolve mentor-reported defects

**Requirement IDs:** REQ-STP-W-403  
**Business value:** Converts external review findings into verified product quality improvements.

**Acceptance criteria**

1. Select at least five mentor-created issues, reproduce each against a recorded baseline, and link the fix to its issue.
2. Each fix contains a regression test at the lowest effective level and does not weaken an existing assertion or requirement.
3. Record before/after behavior and all verification commands/results.

**Dependencies:** Mentor issues must exist; applicable feature stories completed.  
**Impact:** Backend/Frontend/Database—determined strictly by the selected in-scope defects; no unrelated features.  
**Required tests:** At least one regression test per defect plus affected suite/build.  
**Concurrency/transaction/cache/messaging:** Re-test the applicable concern when a defect touches those mechanisms.

### Story 6.3 — Verify the performance target

**Requirement IDs:** REQ-STP-W-404  
**Business value:** Demonstrates that the service sustains the required concurrent request load.

**Acceptance criteria**

1. Run exactly `ab -n 1000 -c 50` against a documented representative authenticated API endpoint; provide a reusable auth/header setup if required.
2. Record hardware/runtime, dataset size, endpoint, cache state, command, timestamp, average latency, throughput, and error count.
3. Average latency is below 200 ms and error rate below 1%; failures produce an in-scope optimization and rerun rather than edited evidence.

**Dependencies:** Stable release candidate and representative data; Story 2.5 query evidence.  
**Impact:** Backend/Frontend/Database—only evidence-driven tuning required to meet the stated target.  
**Required tests:** Performance run plus regression suites after tuning.  
**Concurrency/transaction/cache/messaging:** Test at concurrency 50; state whether the endpoint is cached and ensure mutations, if chosen, cannot corrupt shared test data.

### Story 6.4 — Complete the StockPulse README

**Requirement IDs:** REQ-STP-W-405  
**Business value:** Lets a new reviewer run, understand, test, and troubleshoot the actual delivered system.

**Acceptance criteria**

1. Extend the existing README rather than replace useful starter content; document prerequisites, environment variables, Docker infrastructure, clean startup, backend/frontend commands, seed data, roles, and MailHog/RabbitMQ/Swagger locations.
2. Document the implemented synchronous request layering and asynchronous movement-completed → low-stock → reorder/email flow without claiming unimplemented features.
3. Include API summary, migration order, test/build commands, performance command, known limitations, and troubleshooting based on the final repository.

**Dependencies:** Final implemented stories and verified commands.  
**Impact:** Backend—none; Frontend—none; Database—none; Documentation—README only.  
**Required tests:** Execute or validate every documented command/link; Markdown/diff check.  
**Concurrency/transaction/cache/messaging:** Documentation explains ordered stock locking, atomic completion, five-minute cache/after-commit invalidation, and retry/DLQ behavior.

### Story 6.5 — Prepare and deliver the StockPulse demo

**Requirement IDs:** REQ-STP-W-406  
**Business value:** Communicates the product outcome and technical safeguards clearly within the required review window.

**Acceptance criteria**

1. Prepare a timed 15–20 minute runbook covering problem, architecture already implemented, catalog/location baseline, stock movement, locking, cache, low-stock automation, dashboard, tests, and results.
2. Use deterministic demo accounts/data and include recovery steps for infrastructure or asynchronous timing failures.
3. Assign presenters, rehearse within the time box, and record delivery evidence requested by the training process.

**Dependencies:** Story 5.4 and final release documentation.  
**Impact:** Backend—none; Frontend—none; Database—repeatable demo seed/reset procedure only.  
**Required tests:** Full rehearsal of startup and demo path; verify accounts, data, RabbitMQ, Redis, MailHog, and timing before delivery.  
**Concurrency/transaction/cache/messaging:** Demo explicitly shows or explains the relevant safeguards without modifying their production behavior.

---

## Complete Requirement Traceability

| Requirement ID | Baseline status | Planned disposition |
|---|---|---|
| REQ-STP-B-101 | IMPLEMENTED | Baseline only—no story; Warehouse entity and V2 already satisfy it. |
| REQ-STP-B-102 | IMPLEMENTED | Baseline only—no story; Category entity/self-reference and V2 already satisfy it. |
| REQ-STP-B-103 | IMPLEMENTED | Baseline only—no story; Product entity and V2 already satisfy it. |
| REQ-STP-B-104 | IMPLEMENTED | Baseline only—no story; merged Warehouse/Category services and tests are preserved. |
| REQ-STP-B-105 | PARTIALLY_IMPLEMENTED | Story 1.2—only activate PostgreSQL full-text search. |
| REQ-STP-B-106 | PARTIALLY_IMPLEMENTED | Story 1.1—only add missing endpoint role authorization. |
| REQ-STP-B-107 | NOT_IMPLEMENTED | Story 2.1. |
| REQ-STP-F-101 | PARTIALLY_IMPLEMENTED | Story 1.3—only add Min Stock and Reorder Point columns/data. |
| REQ-STP-F-102 | PARTIALLY_IMPLEMENTED | Story 1.4—only replace category numeric input with dropdown integration. |
| REQ-STP-F-103 | NOT_IMPLEMENTED | Story 1.5. |
| REQ-STP-F-104 | PARTIALLY_IMPLEMENTED | Story 1.5—only add Warehouse route/navigation; Product routing remains baseline. |
| REQ-STP-T-101 | IMPLEMENTED | Baseline only—10 Product service tests already satisfy it. |
| REQ-STP-T-102 | IMPLEMENTED | Baseline only—8 Warehouse service tests already satisfy it. |
| REQ-STP-T-103 | PARTIALLY_IMPLEMENTED | Story 5.2—only complete and record Swagger/OpenAPI verification. |
| REQ-STP-B-201 | IMPLEMENTED | Story 2.2. |
| REQ-STP-B-202 | IMPLEMENTED | Stories 3.1 and 3.2. |
| REQ-STP-B-203 | IMPLEMENTED | Story 3.2. |
| REQ-STP-B-204 | IMPLEMENTED | Story 2.2. |
| REQ-STP-B-205 | IMPLEMENTED | Story 3.3. |
| REQ-STP-B-206 | IMPLEMENTED | Story 2.3. |
| REQ-STP-B-207 | IMPLEMENTED | Story 4.1—StockPulse topology, versioned event contract, and after-commit publisher are complete; retry/DLQ remains B-305. |
| REQ-STP-B-208 | IMPLEMENTED | Story 4.2—committed-stock threshold consumer and versioned low-stock publication are complete. |
| REQ-STP-B-209 | IMPLEMENTED | Stories 4.2 and 4.3—idempotent reorder creation and MailHog email consumer are complete. |
| REQ-STP-F-201 | NOT_IMPLEMENTED | Story 2.4. |
| REQ-STP-F-202 | NOT_IMPLEMENTED | Story 3.4. |
| REQ-STP-F-203 | NOT_IMPLEMENTED | Story 3.4. |
| REQ-STP-F-204 | NOT_IMPLEMENTED | Story 3.5. |
| REQ-STP-T-201 | IMPLEMENTED | Story 3.2. |
| REQ-STP-T-202 | IMPLEMENTED | Story 2.3. |
| REQ-STP-B-301 | NOT_IMPLEMENTED | Story 3.6. |
| REQ-STP-B-302 | NOT_IMPLEMENTED | Story 3.6. |
| REQ-STP-B-303 | NOT_IMPLEMENTED | Story 2.5. |
| REQ-STP-B-304 | NOT_IMPLEMENTED | Story 4.2. |
| REQ-STP-B-305 | NOT_IMPLEMENTED | Story 4.1. |
| REQ-STP-B-306 | NOT_IMPLEMENTED | Story 2.5. |
| REQ-STP-B-307 | PARTIALLY_IMPLEMENTED | Story 5.2—only complete missing StockPulse/response/error documentation. |
| REQ-STP-F-301 | NOT_IMPLEMENTED | Story 4.4. |
| REQ-STP-F-302 | NOT_IMPLEMENTED | Story 4.4. |
| REQ-STP-F-303 | NOT_IMPLEMENTED | Story 5.1. |
| REQ-STP-T-301 | NOT_IMPLEMENTED | Story 5.3. |
| REQ-STP-T-302 | NOT_IMPLEMENTED | Story 3.6. |
| REQ-STP-T-303 | NOT_IMPLEMENTED | Story 4.5. |
| REQ-STP-W-401 | NOT_IMPLEMENTED | Story 5.4. |
| REQ-STP-W-402 | NOT_IMPLEMENTED | Story 6.1. |
| REQ-STP-W-403 | NOT_IMPLEMENTED | Story 6.2. |
| REQ-STP-W-404 | NOT_IMPLEMENTED | Story 6.3. |
| REQ-STP-W-405 | PARTIALLY_IMPLEMENTED | Story 6.4—extend existing README only for missing StockPulse content. |
| REQ-STP-W-406 | NOT_IMPLEMENTED | Story 6.5. |

**Coverage:** 48 of 48 requirement identifiers preserved: 17 implemented, 8 partial requirements scoped to missing work, and 23 not-implemented requirements planned.

## Dependency-Ordered Delivery

1. Story 1.1 and Story 2.1 establish roles and schema.
2. Stories 1.2–1.5 close existing catalog/location gaps while Stories 2.2–2.5 deliver stock visibility.
3. Epic 3 delivers movement creation, atomic completion, APIs, UI, and concurrency proof.
4. Epic 4 adds post-commit messaging, alerts, reorder, email, UI, and DLQ proof.
5. Epic 5 completes dashboard, OpenAPI, integration, and end-to-end validation.
6. Epic 6 supplies externally dependent release evidence, documentation, and demo preparation.
