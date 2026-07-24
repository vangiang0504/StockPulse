---
baseline_commit: bdaa9b6669f8999865f67a456617a60915eca3c6
---

# Story 3.1: Create and inspect stock movements

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a warehouse staff member (STAFF),
I want to create IMPORT, EXPORT, and TRANSFER stock movements with their line items and inspect a movement's details,
so that intended inventory changes are recorded and reviewable **before** any stock quantity is changed.

**Requirement ID:** REQ-STP-B-202 (create portion) — Epic 3, Story 3.1
**Business value:** Records intended inventory changes and their line items before stock is changed.

## Scope Boundaries (read first)

This story delivers the **movement domain layers plus the create + inspect service operations only**. Keep it strictly inside these lines:

- **IN scope:** `MovementType`/`MovementStatus` enums, `StockMovement` + `StockMovementItem` entities, request/response record DTOs, MapStruct mapper, repositories, `StockMovementService` interface + impl with `createImport`, `createExport`, `createTransfer`, and `getById` (inspect a single movement with its items), plus Mockito unit tests.
- **OUT of scope — do NOT build here:**
  - **No stock mutation, no `approve`/`complete`, no pessimistic locking** → that is Story 3.2 (REQ-STP-B-203). Creating or inspecting a movement must never touch `stock_levels`.
  - **No REST controller / no `@PreAuthorize` / no endpoints** → that is Story 3.3 (REQ-STP-B-205). This story stops at the service interface.
  - **No paginated list-with-filters query** → the `GET /movements` list + type/status filters belong to Story 3.3. This story's "inspect" is single-movement detail (`getById`) only.
  - **No cache eviction, no event publication** → nothing here publishes events or evicts Redis (Stories 2.3 / 4.1).
  - **No Flyway migration.** See the migration warning below.

## Acceptance Criteria

1. Implement the movement/item entities, record DTOs, MapStruct mapping, repositories, and service interface/implementation, and define the specified status model `DRAFT → PENDING_APPROVAL → APPROVED → COMPLETED / REJECTED`, plus `CANCELLED`. The enum values must exactly match the V4 `chk_movement_status` check constraint.
2. `createImport`, `createExport`, and `createTransfer` persist a unique `reference_no` and all line items **atomically in one transaction**; every item quantity is positive and every referenced product and warehouse must already exist (otherwise 404).
3. Transfers require **distinct** source and destination warehouses (400 if equal or destination missing); a duplicate product line within a single movement is **rejected** (400), never silently combined.
4. Creating or inspecting a movement **never changes stock**; `createdBy` is stored from the authenticated principal.
5. A newly created movement is persisted with the agreed initial status (see Decision D1), a server-generated unique reference, and `dest_warehouse_id` set only for TRANSFER (NULL otherwise, per the V4 `chk_movement_destination` constraint).
6. `getById` returns the movement with its full item list and enriched display fields (warehouse code/name, per-item product SKU/name); an unknown id returns 404.

## Tasks / Subtasks

- [x] **Task 1 — Enums** (AC: 1)
  - [x] `enums/MovementType.java`: `IMPORT, EXPORT, TRANSFER, ADJUSTMENT` (matches `chk_movement_type`). Plain enum, no bodies — mirror `enums/StockStatus.java`.
  - [x] `enums/MovementStatus.java`: `DRAFT, PENDING_APPROVAL, APPROVED, COMPLETED, REJECTED, CANCELLED` (matches `chk_movement_status`).
- [x] **Task 2 — Entities mapped to the existing V4 tables** (AC: 1, 4, 5)
  - [x] `entity/StockMovement.java` **extends `BaseEntity`** (table `stock_movements` has `created_at` + `updated_at`). Fields: `referenceNo` (`reference_no`, unique, len 50), `type` (`@Enumerated(EnumType.STRING)`), `status` (`@Enumerated(EnumType.STRING)`, `@Builder.Default`), `warehouseId` (`warehouse_id`), `destWarehouseId` (`dest_warehouse_id`, nullable), `notes` (TEXT), `createdBy` (`created_by`), `approvedBy` (`approved_by`, nullable). Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`; `@Entity @Table(name = "stock_movements")`.
  - [x] `entity/StockMovementItem.java` — **plain `@Entity`, does NOT extend `BaseEntity`** (the `stock_movement_items` table has **no** `created_at`/`updated_at`; extending `BaseEntity` causes `Schema-validation: missing column [created_at]` at startup). Declare its own `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`. Fields: `movementId` (`movement_id`, plain `Long`), `productId` (`product_id`, plain `Long`), `quantity` (`Integer`), `unitCost` (`unit_cost`, `BigDecimal`, nullable), `batchNumber` (`batch_number`, nullable), `expiryDate` (`expiry_date`, `LocalDate`, nullable), `notes` (nullable). Precedent: `entity/StockLevel.java`.
- [x] **Task 3 — Repositories** (AC: 2, 6)
  - [x] `repository/StockMovementRepository extends JpaRepository<StockMovement, Long>`: `boolean existsByReferenceNo(String referenceNo)`; optionally `Optional<StockMovement> findByReferenceNo(String)`.
  - [x] `repository/StockMovementItemRepository extends JpaRepository<StockMovementItem, Long>`: `List<StockMovementItem> findByMovementId(Long movementId)` (used by `getById`). Persist item batches via inherited `saveAll(...)`.
- [x] **Task 4 — Request DTOs (records, Bean Validation)** (AC: 2, 3)
  - [x] `dto/request/CreateMovementItemRequest`: `productId` `@NotNull`; `quantity` `@NotNull @Positive`; `unitCost` optional `@DecimalMin(value = "0.0", inclusive = true)`; `batchNumber` optional `@Size(max = 50)`; `expiryDate` optional `LocalDate`; `notes` optional.
  - [x] `dto/request/CreateImportRequest`: `warehouseId` `@NotNull`; `notes` optional; `items` `@NotEmpty @Valid List<CreateMovementItemRequest>`.
  - [x] `dto/request/CreateExportRequest`: identical shape to import (`warehouseId`, `notes`, `items`).
  - [x] `dto/request/CreateTransferRequest`: `warehouseId` (source) `@NotNull`; `destWarehouseId` `@NotNull`; `notes` optional; `items` `@NotEmpty @Valid List<...>`.
  - [x] Keep `@Schema` annotations on fields to match the existing DTO style (e.g. `CreateWarehouseRequest`).
- [x] **Task 5 — Response DTOs (records)** (AC: 6)
  - [x] `dto/response/MovementItemResponse`: `id, productId, productSku, productName, quantity, unitCost, batchNumber, expiryDate, notes`.
  - [x] `dto/response/MovementResponse`: `id, referenceNo, type, status, warehouseId, warehouseCode, warehouseName, destWarehouseId, destWarehouseCode, destWarehouseName, notes, createdBy, approvedBy, List<MovementItemResponse> items, createdAt`. Expose `type`/`status` as `String` (via `.name()`) for frontend filter friendliness, consistent with `UserMapper` mapping `Role` → string. End the record with `createdAt` (not `updatedAt`), matching the established response convention.
- [x] **Task 6 — MapStruct mapper** (AC: 6)
  - [x] `mapper/StockMovementMapper` `@Mapper(componentModel = "spring")`. Follow the **multi-source** precedent in `StockLevelMapper.toResponse(StockLevel, Product, Warehouse)`:
    - `MovementItemResponse toItemResponse(StockMovementItem item, Product product)` with `@Mapping(target = "productSku", source = "product.sku")` etc.
    - `MovementResponse toResponse(StockMovement movement, Warehouse warehouse, Warehouse destWarehouse, List<MovementItemResponse> items)` mapping source/dest code+name; `destWarehouse` may be null for non-transfers, so guard null in the service before calling (pass `null` and map defensively, or assemble those fields in the service).
  - [x] The **service orchestrates** entity fetching (source/dest warehouse, per-item product) then calls the mapper, exactly as `StockLevelServiceImpl.getByWarehouseAndProduct` fetches `Product`/`Warehouse` before mapping.
- [x] **Task 7 — Service interface** (AC: 2, 3, 6)
  - [x] `service/StockMovementService`: `MovementResponse createImport(CreateImportRequest)`, `createExport(CreateExportRequest)`, `createTransfer(CreateTransferRequest)`, `MovementResponse getById(Long id)`.
- [x] **Task 8 — Service implementation** (AC: 2, 3, 4, 5, 6)
  - [x] `service/impl/StockMovementServiceImpl` `@Service @RequiredArgsConstructor @Slf4j`. Inject: `StockMovementRepository`, `StockMovementItemRepository`, `ProductRepository`, `WarehouseRepository`, `UserRepository`, `StockMovementMapper`.
  - [x] Shared create pipeline (one `@Transactional` write) used by import/export/transfer:
    1. Validate the source warehouse exists (`warehouseRepository.existsById` → else `ResourceNotFoundException("Warehouse", id)`).
    2. For transfer only: validate `destWarehouseId` exists and `!destWarehouseId.equals(warehouseId)` → else `BadRequestException`.
    3. **Guard the item list in the service** (do not rely on Bean Validation — unit tests bypass it, see Testing note): items non-empty; each `quantity != null && quantity > 0` (`BadRequestException`); each `productId` exists (`ResourceNotFoundException`); **reject duplicate `productId`** across items (`BadRequestException` — detect with a `Set`).
    4. Resolve `createdBy` from the authenticated principal (see Decision D2).
    5. Generate a unique `referenceNo` (see Decision D3); if `existsByReferenceNo` is true, regenerate within a bounded retry, else 409 `DuplicateResourceException("StockMovement", "referenceNo", value)`.
    6. Build the `StockMovement` (set `type`, initial `status` per D1, `warehouseId`, `destWarehouseId` = null for import/export, `createdBy`), `save(...)` to obtain the id, then build items with the generated `movementId` and `saveAll(...)`.
    7. Assemble and return `MovementResponse` (fetch source/dest warehouse + per-item product, call mapper). Log `debug` on entry and `info` on success (never log tokens/secrets).
  - [x] `getById` `@Transactional(readOnly = true)`: load movement or 404; `findByMovementId`; enrich; return.
- [x] **Task 9 — Unit tests** `service/StockMovementServiceTest` (AC: all) — see the Testing section for the required cases. Keep the suite green: `backend/mvnw.cmd test` (Windows) or `./mvnw test`.

## Dev Notes

### Critical: this is a brownfield delta — reuse, don't recreate

- **The V4 migration already exists and is applied.** `backend/src/main/resources/db/migration/V4__create_movement_tables.sql` created `stock_movements` and `stock_movement_items` back in Story 2.1. **Do NOT write, edit, or add any migration in this story.** Editing an applied migration breaks Flyway (checksum mismatch → app refuses to start); consuming a new version number collides with the reserved V3–V7 range. Your entities must match the existing V4 SQL exactly. `ddl-auto: validate` will fail startup on any mismatch. [Source: AGENTS.md#Flyway-rules; Hard rules 2–4]
- **Follow the exact vertical slice already used by Warehouse/Category/StockLevel:** Entity → Repository → DTOs (request/response records) → MapStruct mapper → Service interface → ServiceImpl → unit tests. [Source: AGENTS.md#Layering; backend-implementation-patterns.md]
- **Foreign keys stay plain `Long`. No `@ManyToOne`, and no `@OneToMany` for the item collection** (Hard rule 7). `StockMovementItem.movementId` and `.productId` are scalar `Long`s exactly like `StockLevel.productId`/`warehouseId` and `Category.parentId`. Persist the parent movement first (to get its generated id), then set `movementId` on each item and `saveAll`. Re-assemble the item list on read via `findByMovementId`. This is why the design is a two-step save inside one transaction rather than a JPA cascade. [Source: AGENTS.md#Entities; entity/StockLevel.java; entity/Category.java]

### The V4 schema you are mapping to (authoritative)

```sql
-- stock_movements
id BIGSERIAL PK
reference_no VARCHAR(50) NOT NULL UNIQUE
type VARCHAR(20) NOT NULL              -- IMPORT, EXPORT, TRANSFER, ADJUSTMENT
status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
warehouse_id BIGINT NOT NULL REFERENCES warehouses(id)
dest_warehouse_id BIGINT REFERENCES warehouses(id)
notes TEXT
created_by BIGINT NOT NULL REFERENCES users(id)
approved_by BIGINT REFERENCES users(id)
created_at / updated_at TIMESTAMP NOT NULL DEFAULT NOW()
-- CHECK chk_movement_type: type IN ('IMPORT','EXPORT','TRANSFER','ADJUSTMENT')
-- CHECK chk_movement_status: status IN ('DRAFT','PENDING_APPROVAL','APPROVED','COMPLETED','REJECTED','CANCELLED')
-- CHECK chk_movement_destination:
--   (type='TRANSFER' AND dest_warehouse_id IS NOT NULL AND dest_warehouse_id <> warehouse_id)
--   OR (type<>'TRANSFER' AND dest_warehouse_id IS NULL)

-- stock_movement_items (NO created_at / updated_at columns)
id BIGSERIAL PK
movement_id BIGINT NOT NULL REFERENCES stock_movements(id)
product_id BIGINT NOT NULL REFERENCES products(id)
quantity INT NOT NULL                 -- CHECK quantity > 0
unit_cost DECIMAL(12,2)               -- CHECK unit_cost IS NULL OR unit_cost >= 0
batch_number VARCHAR(50)
expiry_date DATE
notes TEXT
```

[Source: backend/src/main/resources/db/migration/V4__create_movement_tables.sql; Project 4 - StockPulse.md lines 103–127]

> **The `chk_movement_destination` DB constraint is a hard guard.** If import/export ever sets `dest_warehouse_id`, or a transfer sets it equal to the source, the INSERT fails at the database with a 500 — not a clean 400. Enforce the rule in the service (step 2 above) so the caller gets a proper `BadRequestException` before the row is ever written.

### Decisions to confirm with the human (khoa) — recommended defaults chosen

- **D1 — Initial status on create → recommend `PENDING_APPROVAL`.** The API surface (Story 3.3) has create, list, detail, `approve`, `complete` — there is **no** "submit"/DRAFT-edit endpoint anywhere, and `approve` (Story 3.2) needs a state to act on. Setting the created movement straight to `PENDING_APPROVAL` keeps the lifecycle reachable. `DRAFT` remains in the enum (schema parity) but would be a dead state with no transition trigger. Set the value explicitly via `@Builder.Default private MovementStatus status = MovementStatus.PENDING_APPROVAL;` (do not lean on the SQL `DEFAULT 'DRAFT'`, since JPA inserts the entity's value). *If khoa prefers `DRAFT`-on-create, only the constant and the `status initialization` test change.*
- **D2 — `createdBy` resolution → resolve from `SecurityContextHolder`, look the user up by username.** The authenticated principal is a Spring `org.springframework.security.core.userdetails.User` built with **username only — it carries no database id** (see `security/UserDetailsServiceImpl.java`). So the service does: `String username = SecurityContextHolder.getContext().getAuthentication().getName();` then `userRepository.findByUsername(username)` → `user.getId()` for `createdBy` (404/`IllegalState` if absent — an authenticated request always resolves). **Testability:** this needs no static mocking — unit tests populate the real `ThreadLocal` in `@BeforeEach`, e.g. `SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("staff", null))`, and mock `userRepository.findByUsername("staff")`. Clear the context in `@AfterEach`. *(Alternative if khoa dislikes touching the context in tests: a small injectable `CurrentUserProvider` seam — the same seam would also serve `approvedBy` in Story 3.2. Pick one and stay consistent.)*
- **D3 — `reference_no` is server-generated** (the create form in REQ-STP-F-202 collects warehouse + item rows only, **no reference field**). Recommend a readable, collision-resistant format such as `"%s-%s-%s".formatted(typePrefix, yyyyMMdd, shortRandom)` → e.g. `IMP-20260724-7F3A` (prefixes `IMP`/`EXP`/`TRF`). Verify with `existsByReferenceNo` and regenerate on the rare clash (bounded loop, e.g. 5 tries); the DB `UNIQUE` constraint is the final backstop. *Format is cosmetic — confirm the prefix scheme with khoa.*

### Enum, transaction, and safety notes

- Persist enums as strings: `@Enumerated(EnumType.STRING)`. Ordinal storage would silently corrupt the `VARCHAR` check-constrained columns. [Source: backend-implementation-patterns.md#Entity]
- The whole create is **one** `@Transactional` boundary: movement + all items commit together or not at all (AC 2 "atomically"). A single invalid item (bad product, non-positive qty, duplicate line) must abort before any `save`, so nothing is half-written. [Source: backend-implementation-patterns.md#Service — "A StockPulse write operation that updates multiple rows must have one explicit transaction boundary."]
- `ADJUSTMENT` exists in the type enum/DB for parity but has **no** create method in this story (only import/export/transfer are specified). Leave it unused here.
- `CANCELLED` is defined in the status enum for parity and future use; there is **no** cancel endpoint in the spec API table, so no cancel operation is implemented in this story. Enforcement of *which* transitions are legal (approve/complete/reject) lands in Story 3.2.
- Map service failures to the established exceptions (the `GlobalExceptionHandler` already translates them): missing product/warehouse → `ResourceNotFoundException` (404); duplicate reference → `DuplicateResourceException` (409); broken invariant (non-positive qty, duplicate product line, transfer same/missing dest) → `BadRequestException` (400). Do not add new exception types. [Source: AGENTS.md#Services; exception/*.java]

### Testing standards (Mockito unit tests — the deliverable for this story)

- `@ExtendWith(MockitoExtension.class)`, `@Mock` the repositories + mapper + `UserRepository`, `@InjectMocks` the `StockMovementServiceImpl`. Method names `method_condition_expectedResult`; bodies in `// Given / // When / // Then`; assert with AssertJ. Mockito runs strict — stub only what each test uses. Model the file on `service/CategoryServiceTest.java`. [Source: AGENTS.md#Testing]
- **Critical — the service must self-guard invariants, because unit tests bypass Bean Validation.** Unit tests instantiate the service directly (no Spring MVC), so `@NotNull`/`@Positive`/`@NotEmpty`/`@Valid` on the DTOs are **not** enforced in these tests. Every invariant the ACs require you to test (positive quantity, non-empty items, duplicate product line, transfer distinct/non-null destination) must **also** be an explicit service-side check throwing `BadRequestException`. The DTO annotations stay as the controller's first line of defense in Story 3.3; the service checks are what make the required unit tests meaningful.
- Required cases (cover the happy path plus every guard): `createImport_validRequest_persistsMovementAndItems`, `createExport_validRequest_persists`, `createTransfer_validRequest_persists`, `create_unknownProduct_throwsResourceNotFound`, `create_unknownWarehouse_throwsResourceNotFound`, `create_nonPositiveQuantity_throwsBadRequest`, `create_duplicateProductLine_throwsBadRequest`, `createTransfer_sameSourceAndDestination_throwsBadRequest`, `createTransfer_missingDestination_throwsBadRequest`, `create_duplicateReference_throwsDuplicateResource` (stub `existsByReferenceNo` → true), `create_setsInitialStatus_pendingApproval`, `create_storesCreatedByFromPrincipal`, `create_invalidItemAbortsBeforeSave` (`verify(...Repository, never()).saveAll(any())`), `getById_found_returnsMovementWithItems`, `getById_notFound_throwsResourceNotFound`.
- Integration/concurrency/DB-backed proof is **not** in this story — it is Story 3.6 (REQ-STP-B-301/302). Do not add Testcontainers tests here.

### Project Structure Notes

- All new files live under `backend/src/main/java/com/training/starter/` in the existing packages: `enums/`, `entity/`, `repository/`, `dto/request/`, `dto/response/`, `mapper/`, `service/`, `service/impl/`. Test under `backend/src/test/java/com/training/starter/service/`. No new packages, no config changes.
- MapStruct impls are generated at build into `target/generated-sources/annotations/`; run `backend/mvnw.cmd compile` if the IDE cannot resolve `StockMovementMapperImpl`.
- Pure backend story: **no frontend, no `SecurityConfig`, no `RabbitMQConfig`/`RedisConfig`, no migration, no controller** touched. The response contract you define here is the input for Stories 3.4/3.5 (movement forms/detail) and is consumed by Story 3.3's controller.
- Coordinate with giang-hv before starting: giang built StockLevel (Story 2.2, commit `51d6ec3`). This story adds only new movement files, so a merge conflict is unlikely — but the shared enums/entity package is where both developers work, so pull `main` clean before branching. [Source: AGENTS.md#Git-conventions — keep the tree clean before pulling]
- Git: branch off `main`; commit messages prefixed `khoa-nxd:`; run `mvnw test` before pushing; never commit `backend/target/`. [Source: AGENTS.md#Git-conventions]

### References

- [Source: Project 4 - StockPulse.md#Stock-Movement-Status-Flow] — `DRAFT → PENDING_APPROVAL → APPROVED → COMPLETED / REJECTED`, `→ CANCELLED` (lines 205–210).
- [Source: Project 4 - StockPulse.md#API-Endpoints — Stock Movements] — endpoint/role table (lines 249–258); confirms server-owned reference (no reference field on create) and the STAFF/MANAGER split for Story 3.3.
- [Source: Project 4 - StockPulse.md — V4 DDL] — lines 103–127; [REQ-STP-B-202 checklist item] line 358.
- [Source: _bmad-output/planning-artifacts/epics.md#Story-3.1] — acceptance criteria, dependencies, required tests (lines 210–225).
- [Source: backend/src/main/resources/db/migration/V4__create_movement_tables.sql] — the exact columns/constraints to map to.
- [Source: backend/src/main/java/com/training/starter/entity/StockLevel.java] — precedent for a stock entity that does **not** extend `BaseEntity` (no `created_at`), `@Version`, explicit `@Column` names.
- [Source: backend/src/main/java/com/training/starter/entity/Category.java] — precedent for scalar `Long` FK (`parentId`) and `BaseEntity` extension.
- [Source: backend/src/main/java/com/training/starter/service/impl/CategoryServiceImpl.java] — precedent for `existsByCode`/`existsById` guard-before-save, duplicate/not-found exception mapping, `@Transactional` write.
- [Source: backend/src/main/java/com/training/starter/service/impl/StockLevelServiceImpl.java] — precedent for fetching `Product`+`Warehouse` then multi-source mapping.
- [Source: backend/src/main/java/com/training/starter/mapper/StockLevelMapper.java] — multi-source `@Mapping` pattern for enriched responses; [dto/response/StockLevelResponse.java] — flat record shape.
- [Source: backend/src/main/java/com/training/starter/security/UserDetailsServiceImpl.java] — proves the principal carries username only, no id.
- [Source: backend/src/test/java/com/training/starter/service/CategoryServiceTest.java] — the unit-test template to copy (structure, `verify(..., never()).save(...)` on guard paths).
- [Source: _bmad-output/project-documentation/backend-implementation-patterns.md] — layering, DTO/mapper/service/controller conventions, transaction-boundary rule.
- [Source: AGENTS.md] — Hard rules (no migration edits, no V3–V7 reuse, no DELETE, no `@ManyToOne`), entity/DTO/mapper/service conventions, testing conventions.

## Dev Agent Record

### Agent Model Used

claude-opus-4-8 (Claude Code / bmad-dev-story)

### Debug Log References

- `backend/mvnw.cmd test -Dtest=StockMovementServiceTest` → **15/15 passed**, BUILD SUCCESS.
- `backend/mvnw.cmd test` (full suite) → **87/87 unit tests passed**; 4 Testcontainers integration tests (`FlywayMigrationIntegrationTest`, `ProductSearchIntegrationTest`, `StockLevelRepositoryIntegrationTest`, `CatalogWarehouseAuthorizationIntegrationTest`) error with *"Could not find a valid Docker environment"*. This is a pre-existing Testcontainers↔Docker Desktop connectivity issue in this environment (the active Docker context is `desktop-linux`, whose pipe Testcontainers does not auto-discover); it is unrelated to this story's additive changes — none of those 4 tests reference movement code.
- Entity↔schema alignment verified directly against the live `training-postgres` V4 tables via `\d stock_movements` / `\d stock_movement_items`: every column name/type/nullability and the `chk_movement_type` / `chk_movement_status` / `chk_movement_destination` constraints match the entities. `StockMovement` maps id/created_at/updated_at (`BaseEntity`); `StockMovementItem` has none and is a plain entity — as required.

### Completion Notes List

- Implemented the full movement create + inspect vertical slice (enums → entities → repositories → DTOs → MapStruct mapper → service interface/impl → unit tests). No Flyway migration authored — mapped to the existing applied V4.
- Confirmed decisions applied: **D1** initial status `PENDING_APPROVAL` (set explicitly on the entity, overriding the SQL `DRAFT` default); **D2** `createdBy` resolved from `SecurityContextHolder` username → `UserRepository.findByUsername` (unit tests populate the security context in `@BeforeEach`); **D3** server-generated reference `IMP|EXP|TRF-yyyyMMdd-XXXX` with `existsByReferenceNo` retry and a 409 backstop.
- Service self-guards every invariant (non-positive quantity, empty items, duplicate product line, transfer distinct/non-null destination, product/warehouse existence) so the required unit tests are meaningful without Bean Validation; DTO constraints remain for the controller in Story 3.3.
- Creation is a single `@Transactional`: movement saved first (to obtain its id), items linked by scalar `movementId` and `saveAll`-ed — no `@ManyToOne`/`@OneToMany`, no stock mutation, no cache/event side effects.
- Left in scope for later stories as designed: approve/complete + stock mutation + locking (3.2), REST controller + `@PreAuthorize` + paginated list/filters (3.3).

### File List

New (backend/src/main/java/com/training/starter/):
- `enums/MovementType.java`
- `enums/MovementStatus.java`
- `entity/StockMovement.java`
- `entity/StockMovementItem.java`
- `repository/StockMovementRepository.java`
- `repository/StockMovementItemRepository.java`
- `dto/request/CreateMovementItemRequest.java`
- `dto/request/CreateImportRequest.java`
- `dto/request/CreateExportRequest.java`
- `dto/request/CreateTransferRequest.java`
- `dto/response/MovementItemResponse.java`
- `dto/response/MovementResponse.java`
- `mapper/StockMovementMapper.java`
- `service/StockMovementService.java`
- `service/impl/StockMovementServiceImpl.java`

New (backend/src/test/java/com/training/starter/):
- `service/StockMovementServiceTest.java`

Modified:
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (story/epic status)
- `_bmad-output/implementation-artifacts/3-1-create-and-inspect-stock-movements.md` (this story)

## Change Log

| Date | Change |
|------|--------|
| 2026-07-24 | Implemented REQ-STP-B-202 create+inspect movement layers (16 new source/test files); 15 new unit tests; all 87 unit tests green. Status → review. |
