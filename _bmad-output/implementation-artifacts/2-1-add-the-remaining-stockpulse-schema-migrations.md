# Story 2.1: Add the Remaining StockPulse Schema Migrations

Status: ready-for-dev

## Story

As a StockPulse development team,
I want the remaining inventory schema delivered through immutable Flyway migrations,
so that stock, movement, alert, reorder, search, and reporting features share a durable PostgreSQL foundation.

## Acceptance Criteria

1. The released `V1__create_users_table.sql` and `V2__create_warehouse_tables.sql` files are byte-for-byte unchanged. New immutable migrations are named exactly `V3__create_stock_tables.sql`, `V4__create_movement_tables.sql`, `V5__create_alert_tables.sql`, `V6__create_indexes.sql`, and `V7__create_materialized_views.sql`; the category seed is added as `R__insert_dummy_category.sql` and consumes no version.
2. V3 creates `stock_levels` with its specified columns, defaults, foreign keys, composite uniqueness, non-negative quantity checks, optimistic-lock version, and update timestamp.
3. V4 creates `stock_movements` and `stock_movement_items` with the specified columns, foreign keys, enum-compatible string checks, lifecycle defaults, positive item quantity, non-negative optional unit cost, timestamps, and transfer destination consistency.
4. V5 creates `stock_alerts` and `reorder_suggestions` with the specified columns, foreign keys, enum-compatible string checks, non-negative stock/threshold values, positive suggested quantity, timestamps, and database uniqueness for an unresolved alert or pending reorder candidate.
5. V6 backfills every existing Product `search_vector`, installs insert/update maintenance for SKU/name changes, and creates all specified B-tree, partial, and GIN indexes. The vector and query contract use PostgreSQL's `simple` text-search configuration consistently so SKU/name tokens are case-insensitive and language stemming does not alter inventory identifiers.
6. V7 creates `mv_stock_summary` with exactly one row per active Product/Warehouse stock level, the specified derived availability and status values, and a unique `(product_id, warehouse_id)` index that permits `REFRESH MATERIALIZED VIEW CONCURRENTLY` after the view has first been populated.
7. SQL string values establish the exact future JPA enum contract: movement type `IMPORT|EXPORT|TRANSFER|ADJUSTMENT`; movement status `DRAFT|PENDING_APPROVAL|APPROVED|COMPLETED|REJECTED|CANCELLED`; alert type `LOW_STOCK|OUT_OF_STOCK|OVERSTOCK`; alert status `ACTIVE|ACKNOWLEDGED|RESOLVED`; reorder status `PENDING|APPROVED|REJECTED`. Future entities must use `EnumType.STRING` and fields/constraints matching these migrations; no entity, repository, service, controller, or frontend code is part of this story.
8. A dedicated Flyway-enabled PostgreSQL 16 integration suite proves both a clean V1-to-V7 migration and an upgrade from an already-populated V2 database without data loss. It also verifies Flyway validation on a second startup, the required tables/columns/defaults/constraints/indexes, search-vector backfill and maintenance, materialized-view results and concurrent-refresh index, and repeatable-seed idempotency.
9. The existing application test profile, `BaseIntegrationTest`, application behavior, and Product search service/repository delegation remain unchanged. Story 1.2 owns switching the runtime Product search path after V6 is available.

## Tasks / Subtasks

- [ ] Protect the released migration baseline (AC: 1, 8)
  - [ ] Record the current V1 and V2 checksums in the migration test or a test fixture before adding new migrations; do not edit, rename, reformat, or move either file.
  - [ ] Confirm the V2-upgrade test inserts sentinel User, Category, Warehouse, and Product rows before migrating beyond V2 and asserts those exact rows remain afterward.
  - [ ] Keep schema ownership with Flyway; do not use Hibernate DDL or test-only SQL to create the production objects under test.
- [ ] Add V3 stock-level schema (AC: 2, 7)
  - [ ] Create `V3__create_stock_tables.sql` exactly as specified in the V3 contract below.
  - [ ] Verify composite uniqueness, foreign keys, defaults, non-negative quantities, version, and timestamp behavior in PostgreSQL.
- [ ] Add V4 movement schema (AC: 3, 7)
  - [ ] Create `V4__create_movement_tables.sql` exactly as specified in the V4 contract below.
  - [ ] Verify reference uniqueness, allowed type/status strings, transfer destination consistency, positive item quantity, optional non-negative unit cost, and all foreign keys.
- [ ] Add V5 alert and reorder schema (AC: 4, 7)
  - [ ] Create `V5__create_alert_tables.sql` exactly as specified in the V5 contract below.
  - [ ] Verify allowed enum strings, numeric checks, timestamps, and one unresolved/pending record per Product/Warehouse condition.
- [ ] Add V6 search maintenance and indexes (AC: 5, 8)
  - [ ] Create `V6__create_indexes.sql` exactly as specified in the V6 contract below.
  - [ ] Backfill pre-existing Products before creating the GIN index.
  - [ ] Install a trigger that recomputes the weighted vector only when `sku` or `name` is inserted or changed.
  - [ ] Verify required ordinary, partial, unique-partial, and GIN indexes through PostgreSQL catalogs.
- [ ] Add V7 reporting view (AC: 6, 8)
  - [ ] Create `V7__create_materialized_views.sql` exactly as specified in the V7 contract below.
  - [ ] Verify view projections/status boundaries and the unique index required for concurrent refresh.
- [ ] Add repeatable seed compatibility (AC: 1, 8)
  - [ ] Add `R__insert_dummy_category.sql` with the ten V2 category IDs/codes and `ON CONFLICT (id) DO UPDATE` for deterministic reruns; update `categories_id_seq` after the upsert.
  - [ ] Do not remove the existing V2 seed. Prove the repeatable migration is idempotent and does not duplicate or erase user-created categories.
- [ ] Add dedicated migration integration coverage (AC: 1-8)
  - [ ] Add the PostgreSQL-only test harness and cases defined in the Test Contract below.
  - [ ] Run the migration suite against `postgres:16-alpine`, then run the complete backend tests.
  - [ ] Confirm no production Java, application YAML, existing test-profile, frontend, or unrelated build changes are required.

## Dev Notes

### Scope and source-of-truth decisions

- This story implements only database migrations and their PostgreSQL integration tests. It deliberately does not create the future StockLevel, Movement, Alert, or Reorder JPA layers.
- Current executable state has only V1 and V2. V1 owns `users`; V2 owns `warehouses`, `categories`, `products`, and the inline ten-category seed. Both are released and immutable.
- `products.search_vector TSVECTOR` already exists in V2 but is nullable and unmaintained. `ProductRepository.searchByVector(...)` exists, while `ProductServiceImpl.search(...)` still uses the case-insensitive substring repository method. V6 supplies database readiness only; Story 1.2 changes the application search path.
- The brief's raw DDL is the base contract. The accepted project-context rule that quantities/thresholds cannot be negative, movement item quantity must be positive, and reorder quantity must be at least one is enforced with named `CHECK` constraints below.
- Use `TIMESTAMP` consistently with V1/V2 and `BaseEntity`; do not silently change the project to `TIMESTAMPTZ` in this story.
- Foreign keys use PostgreSQL's default `NO ACTION` behavior. Do not add cascading deletes: inventory history and operational records must not disappear when a parent deletion is attempted.

### Exact V3 contract — `V3__create_stock_tables.sql`

Create `stock_levels` in this column order:

| Column | Exact SQL contract |
|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |
| `product_id` | `BIGINT NOT NULL REFERENCES products(id)` |
| `warehouse_id` | `BIGINT NOT NULL REFERENCES warehouses(id)` |
| `quantity` | `INT NOT NULL DEFAULT 0` |
| `reserved_quantity` | `INT NOT NULL DEFAULT 0` |
| `version` | `BIGINT NOT NULL DEFAULT 0` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |

Add these named constraints:

- `uk_stock_product_warehouse UNIQUE (product_id, warehouse_id)`
- `chk_stock_quantity_non_negative CHECK (quantity >= 0)`
- `chk_stock_reserved_quantity_non_negative CHECK (reserved_quantity >= 0)`
- `chk_stock_version_non_negative CHECK (version >= 0)`

Do not add `created_at` to `stock_levels`; the accepted schema specifies only `updated_at`. Do not enforce `reserved_quantity <= quantity`: reservation semantics are not defined by REQ-STP-B-107 and later stock services may manage physical and reserved quantities independently.

### Exact V4 contract — `V4__create_movement_tables.sql`

Create `stock_movements` in this column order:

| Column | Exact SQL contract |
|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |
| `reference_no` | `VARCHAR(50) NOT NULL UNIQUE` |
| `type` | `VARCHAR(20) NOT NULL` |
| `status` | `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` |
| `warehouse_id` | `BIGINT NOT NULL REFERENCES warehouses(id)` |
| `dest_warehouse_id` | `BIGINT REFERENCES warehouses(id)` |
| `notes` | `TEXT` |
| `created_by` | `BIGINT NOT NULL REFERENCES users(id)` |
| `approved_by` | `BIGINT REFERENCES users(id)` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |

Add these named constraints:

- `chk_movement_type CHECK (type IN ('IMPORT', 'EXPORT', 'TRANSFER', 'ADJUSTMENT'))`
- `chk_movement_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'REJECTED', 'CANCELLED'))`
- `chk_movement_destination CHECK ((type = 'TRANSFER' AND dest_warehouse_id IS NOT NULL AND dest_warehouse_id <> warehouse_id) OR (type <> 'TRANSFER' AND dest_warehouse_id IS NULL))`

Create `stock_movement_items` in this column order:

| Column | Exact SQL contract |
|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |
| `movement_id` | `BIGINT NOT NULL REFERENCES stock_movements(id)` |
| `product_id` | `BIGINT NOT NULL REFERENCES products(id)` |
| `quantity` | `INT NOT NULL` |
| `unit_cost` | `DECIMAL(12,2)` |
| `batch_number` | `VARCHAR(50)` |
| `expiry_date` | `DATE` |
| `notes` | `TEXT` |

Add these named constraints:

- `chk_movement_item_quantity_positive CHECK (quantity > 0)`
- `chk_movement_item_unit_cost_non_negative CHECK (unit_cost IS NULL OR unit_cost >= 0)`

Do not add timestamps or a uniqueness rule to movement items; neither is part of the accepted schema.

### Exact V5 contract — `V5__create_alert_tables.sql`

Create `stock_alerts` in this column order:

| Column | Exact SQL contract |
|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |
| `product_id` | `BIGINT NOT NULL REFERENCES products(id)` |
| `warehouse_id` | `BIGINT NOT NULL REFERENCES warehouses(id)` |
| `alert_type` | `VARCHAR(20) NOT NULL` |
| `current_quantity` | `INT NOT NULL` |
| `threshold` | `INT NOT NULL` |
| `status` | `VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |
| `resolved_at` | `TIMESTAMP` |

Add these named constraints:

- `chk_stock_alert_type CHECK (alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK', 'OVERSTOCK'))`
- `chk_stock_alert_status CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'))`
- `chk_stock_alert_current_quantity_non_negative CHECK (current_quantity >= 0)`
- `chk_stock_alert_threshold_non_negative CHECK (threshold >= 0)`
- `chk_stock_alert_resolution CHECK ((status = 'RESOLVED' AND resolved_at IS NOT NULL) OR (status <> 'RESOLVED' AND resolved_at IS NULL))`

Create `reorder_suggestions` in this column order:

| Column | Exact SQL contract |
|---|---|
| `id` | `BIGSERIAL PRIMARY KEY` |
| `product_id` | `BIGINT NOT NULL REFERENCES products(id)` |
| `warehouse_id` | `BIGINT NOT NULL REFERENCES warehouses(id)` |
| `suggested_quantity` | `INT NOT NULL` |
| `current_stock` | `INT NOT NULL` |
| `reorder_point` | `INT NOT NULL` |
| `status` | `VARCHAR(20) NOT NULL DEFAULT 'PENDING'` |
| `created_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |
| `updated_at` | `TIMESTAMP NOT NULL DEFAULT NOW()` |

Add these named constraints:

- `chk_reorder_suggested_quantity_positive CHECK (suggested_quantity >= 1)`
- `chk_reorder_current_stock_non_negative CHECK (current_stock >= 0)`
- `chk_reorder_point_non_negative CHECK (reorder_point >= 0)`
- `chk_reorder_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))`

The one-unresolved-condition rules are partial unique indexes in V6, not table constraints in V5, because PostgreSQL predicates cannot be expressed by a standard `UNIQUE` table constraint.

### Exact V6 contract — `V6__create_indexes.sql`

Execute in this order:

1. Backfill all rows with:
   `UPDATE products SET search_vector = setweight(to_tsvector('simple', coalesce(sku, '')), 'A') || setweight(to_tsvector('simple', coalesce(name, '')), 'B');`
2. Create `products_search_vector_update()` returning `trigger` in `plpgsql`; assign the same weighted expression to `NEW.search_vector` and return `NEW`.
3. Create `trg_products_search_vector_update BEFORE INSERT OR UPDATE OF sku, name ON products FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();`.
4. Make the populated contract explicit with `ALTER TABLE products ALTER COLUMN search_vector SET NOT NULL` only after the backfill and trigger are installed.
5. Create exactly these indexes:

| Index | Definition |
|---|---|
| `idx_products_sku` | `products(sku)` |
| `idx_products_category` | `products(category_id)` |
| `idx_products_search` | `products USING GIN(search_vector)` |
| `idx_stock_product` | `stock_levels(product_id)` |
| `idx_stock_warehouse` | `stock_levels(warehouse_id)` |
| `idx_stock_low` | `stock_levels(quantity) WHERE quantity < 20` |
| `idx_movements_warehouse` | `stock_movements(warehouse_id)` |
| `idx_movements_type_status` | `stock_movements(type, status)` |
| `idx_movements_created` | `stock_movements(created_at)` |
| `idx_movement_items_product` | `stock_movement_items(product_id)` |
| `idx_alerts_active` | `stock_alerts(status) WHERE status = 'ACTIVE'` |
| `uk_stock_alert_unresolved` | unique on `stock_alerts(product_id, warehouse_id, alert_type) WHERE status IN ('ACTIVE', 'ACKNOWLEDGED')` |
| `uk_reorder_suggestion_pending` | unique on `reorder_suggestions(product_id, warehouse_id) WHERE status = 'PENDING'` |

The explicit `idx_products_sku` is retained because the v1.1 specification names it, even though V2's unique SKU already creates a supporting unique B-tree index. Do not use `CREATE INDEX CONCURRENTLY` inside a versioned Flyway migration because it cannot run in Flyway's normal transaction.

### Exact V7 contract — `V7__create_materialized_views.sql`

Create `mv_stock_summary` from `stock_levels sl`, joining `products p` and `warehouses w`, and left-joining `categories c`. Project exactly:

- `p.id AS product_id`, `p.sku`, `p.name AS product_name`
- `c.name AS category_name`
- `w.id AS warehouse_id`, `w.name AS warehouse_name`
- `sl.quantity`, `sl.reserved_quantity`
- `sl.quantity - sl.reserved_quantity AS available_quantity`
- `p.min_stock`, `p.reorder_point`
- `CASE WHEN sl.quantity = 0 THEN 'OUT_OF_STOCK' WHEN sl.quantity <= p.min_stock THEN 'LOW_STOCK' WHEN sl.quantity >= p.max_stock THEN 'OVERSTOCK' ELSE 'NORMAL' END AS stock_status`

Filter with `WHERE p.active = TRUE`. Then create `CREATE UNIQUE INDEX idx_mv_stock_summary ON mv_stock_summary(product_id, warehouse_id);`. Do not add a scheduler or refresh service in this story.

### Repeatable seed contract — `R__insert_dummy_category.sql`

- Preserve the ten IDs, names, codes, and parent relationships already present in V2.
- Use a single parent-safe insert/upsert order: roots before children.
- On ID conflict, update `name`, `code`, and `parent_id` from `EXCLUDED`; do not delete any rows and do not truncate the table.
- Finish with `SELECT setval('categories_id_seq', COALESCE((SELECT MAX(id) FROM categories), 1));`.
- Because the current V2 seed remains, the first repeatable execution must converge on the same ten rows without duplicates. A later checksum-triggered rerun must do the same while retaining unrelated user-created categories.

### PostgreSQL integration Test Contract

Add `backend/src/test/java/com/training/starter/migration/FlywayMigrationIntegrationTest.java` and any test-only migration location/fixture strictly needed to exercise the V2 baseline. Do not extend `BaseIntegrationTest`: it starts Redis and RabbitMQ unnecessarily, and the `test` profile disables Flyway and uses `ddl-auto: create-drop`.

Use one static `PostgreSQLContainer<?>` with `postgres:16-alpine`, create isolated schemas/databases per scenario, and construct Flyway programmatically with the production `classpath:db/migration` location. Tests must inspect behavior and PostgreSQL catalogs rather than merely checking that startup did not throw.

Required cases:

1. **Clean migration and restart validation** — migrate an empty schema to latest; assert Flyway reports V1–V7 success and the repeatable migration success; call `validateWithResult()` using a new Flyway instance and assert validation succeeds with no invalid migrations.
2. **V2 upgrade without data loss** — migrate a separate schema with `target("2")`; insert sentinel User, Category, Warehouse, and Product data (including a Product with null `search_vector`); migrate to latest; assert sentinel values and V1/V2 seed rows are unchanged, V3–V7 are applied, and the Product vector is populated.
3. **Schema metadata** — query `information_schema.columns`, `table_constraints`, `check_constraints`, `referential_constraints`, `pg_indexes`, `pg_matviews`, and `pg_trigger` to assert the exact tables, columns, nullability/defaults, named checks, FKs, uniqueness, indexes, materialized view, trigger, and `search_vector NOT NULL` contract.
4. **Constraint behavior** — use valid parent fixtures, then assert PostgreSQL rejects: duplicate Product/Warehouse stock rows; negative stock/reserved/version; missing FK parents; invalid enum strings; invalid transfer destination combinations; zero/negative movement item quantities; negative unit cost; negative alert values; inconsistent resolved timestamp; non-positive suggested quantity; negative reorder values; duplicate unresolved alerts; and duplicate pending suggestions. Also prove resolved/rejected historical rows allow a new active/pending row.
5. **Search-vector behavior** — verify V2-upgrade backfill; insert mixed-case SKU/name and assert `search_vector @@ plainto_tsquery('simple', ?)` matches SKU and name terms case-insensitively; update name and assert the old term no longer matches while the new term does; assert unrelated terms return no rows.
6. **Materialized-view behavior** — insert active/inactive Products and stock rows covering zero, low boundary, overstock boundary, and normal stock; refresh the view; assert inactive Products are excluded and every projection/derived status is exact. After the view has data and the unique index exists, execute `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_summary` outside a transaction and assert it succeeds.
7. **Repeatable seed idempotency** — after the initial migrate, insert a user category, run migrate again with an unchanged repeatable checksum, and verify no duplicate seed rows and no user-row loss. For deterministic rerun coverage, invoke the repeatable SQL through a test-only copied location with a checksum change or execute the same idempotent seed fixture twice; never edit the production repeatable file during a test.

Use AssertJ/JUnit 5 and JDBC/Flyway APIs already available through the project dependencies. If direct Flyway API compilation reveals PostgreSQL database support is split from `flyway-core` for the resolved Flyway version, add only the official `org.flywaydb:flyway-database-postgresql` runtime/test dependency needed by that version; do not upgrade unrelated dependencies.

### Exact affected files

Add these production migration files only:

- `backend/src/main/resources/db/migration/V3__create_stock_tables.sql`
- `backend/src/main/resources/db/migration/V4__create_movement_tables.sql`
- `backend/src/main/resources/db/migration/V5__create_alert_tables.sql`
- `backend/src/main/resources/db/migration/V6__create_indexes.sql`
- `backend/src/main/resources/db/migration/V7__create_materialized_views.sql`
- `backend/src/main/resources/db/migration/R__insert_dummy_category.sql`

Add this primary test file:

- `backend/src/test/java/com/training/starter/migration/FlywayMigrationIntegrationTest.java`

Modify `backend/pom.xml` only if the resolved Flyway API requires the official PostgreSQL database module. Test-only resources/helpers may be added under `backend/src/test/resources/db/migration-test/` or the migration test package when necessary for isolated V2-upgrade/repeatable-rerun verification.

Explicitly do not modify:

- `backend/src/main/resources/db/migration/V1__create_users_table.sql`
- `backend/src/main/resources/db/migration/V2__create_warehouse_tables.sql`
- Production Java, entity, DTO, mapper, repository, service, controller, security, cache, messaging, scheduler, or frontend files
- `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-test.yml`, or `BaseIntegrationTest`

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Story 2.1, lines 118-133]
- [Source: `_bmad-output/project-context.md` — source-of-truth order, current migration/search gaps, REQ-STP-B-107 status, and database rules, lines 5-13, 33-40, 63-65, 75-87, 182-196, 220-226]
- [Source: `task.md` — REQ-STP-B-107 remaining work and Story 1.2 dependency, lines 23-50]
- [Source: `Project 4 - StockPulse.md` v1.1 — accepted schema, V3-V7 DDL, repeatable seed rationale, and movement lifecycle, lines 9-31 and 91-209]
- [Source: current `V1__create_users_table.sql` and `V2__create_warehouse_tables.sql` — immutable executable baseline]
- [Source: current `application.yml`, `application-test.yml`, `BaseIntegrationTest`, and `pom.xml` — Flyway/JPA/Testcontainers test behavior]
- [Source: current `Product`, `ProductRepository`, and `ProductServiceImpl` — existing vector column mapping, dormant native vector query, and active substring-search path]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

