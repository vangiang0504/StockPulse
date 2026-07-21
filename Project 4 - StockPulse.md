---
tags:
  - training
  - project
  - stockpulse
  - nhom-4
created: 2026-07-19
updated: 2026-07-21
version: 1.1
---

# Project 4 — StockPulse (Inventory & Stock Management)

**Team**: 4 (2 people)
**Domain**: Warehouse/Inventory — stock management, import/export, low-stock alerts
**Prefix**: STP
Repo: https://github.com/TuanHoAnh/StockPulse

## Change Log

This spec is updated whenever the document conflicts with the actual implementation.
**The "Weekly Requirements" section is kept unchanged** so the training roadmap stays intact.

### v1.1 — 2026-07-21

| # | Change | Reason |
|---|--------|--------|
| 1 | Added `updated_at` to `warehouses` and `categories` (inside V2) | REQ-STP-B-104 requires **CRUD** (including Update), but the original schema had no column to record modification time. Added directly into V2 so versions V3–V7 stay reserved for the stock tables. |
| 2 | Added a **Categories** section to the API table | REQ-STP-B-106 requires endpoints for categories, but the original API table omitted them entirely. |
| 3 | **Warehouses**: added `GET /{id}` and `PUT /{id}` | The original table only listed list + create — not enough for the CRUD that REQ-STP-B-104 demands. |
| 4 | Documented `R__insert_dummy_category.sql` | Dev seed data, implemented as a Flyway *repeatable* migration so it does not consume version V3. |
| 5 | Added **Design Decisions & Known Gaps** | Records design choices and the gap between this document and the current code. |

### v1.0 — 2026-07-19

Original spec from mentor.

## Business Description

Warehouse inventory management system:
- Product and category management
- Warehouse and storage location management
- Stock receiving (import)
- Stock shipping (export)
- Low-stock alerts
- Auto-generated reorder suggestions
- Stock summary reports (materialized view)

## Database Schema

### Core Tables

```sql
-- V2__create_warehouse_tables.sql (V1 is users from starter)
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()   -- v1.1: required by CRUD (REQ-STP-B-104)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()   -- v1.1: required by CRUD (REQ-STP-B-104)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    min_stock INT NOT NULL DEFAULT 10,
    max_stock INT NOT NULL DEFAULT 1000,
    reorder_point INT NOT NULL DEFAULT 20,
    reorder_quantity INT NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    search_vector TSVECTOR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- V3__create_stock_tables.sql
CREATE TABLE stock_levels (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_product_warehouse UNIQUE (product_id, warehouse_id)
);

-- V4__create_movement_tables.sql
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    reference_no VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,          -- IMPORT, EXPORT, TRANSFER, ADJUSTMENT
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    dest_warehouse_id BIGINT REFERENCES warehouses(id),
    notes TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_movement_items (
    id BIGSERIAL PRIMARY KEY,
    movement_id BIGINT NOT NULL REFERENCES stock_movements(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2),
    batch_number VARCHAR(50),
    expiry_date DATE,
    notes TEXT
);

-- V5__create_alert_tables.sql
CREATE TABLE stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    alert_type VARCHAR(20) NOT NULL,    -- LOW_STOCK, OUT_OF_STOCK, OVERSTOCK
    current_quantity INT NOT NULL,
    threshold INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE TABLE reorder_suggestions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    suggested_quantity INT NOT NULL,
    current_stock INT NOT NULL,
    reorder_point INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- V6__create_indexes.sql
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_search ON products USING GIN(search_vector);
CREATE INDEX idx_stock_product ON stock_levels(product_id);
CREATE INDEX idx_stock_warehouse ON stock_levels(warehouse_id);
CREATE INDEX idx_stock_low ON stock_levels(quantity) WHERE quantity < 20;
CREATE INDEX idx_movements_warehouse ON stock_movements(warehouse_id);
CREATE INDEX idx_movements_type_status ON stock_movements(type, status);
CREATE INDEX idx_movements_created ON stock_movements(created_at);
CREATE INDEX idx_movement_items_product ON stock_movement_items(product_id);
CREATE INDEX idx_alerts_active ON stock_alerts(status) WHERE status = 'ACTIVE';

-- V7__create_materialized_views.sql
CREATE MATERIALIZED VIEW mv_stock_summary AS
SELECT
    p.id as product_id, p.sku, p.name as product_name,
    c.name as category_name,
    w.id as warehouse_id, w.name as warehouse_name,
    sl.quantity, sl.reserved_quantity,
    sl.quantity - sl.reserved_quantity as available_quantity,
    p.min_stock, p.reorder_point,
    CASE
        WHEN sl.quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN sl.quantity <= p.min_stock THEN 'LOW_STOCK'
        WHEN sl.quantity >= p.max_stock THEN 'OVERSTOCK'
        ELSE 'NORMAL'
    END as stock_status
FROM stock_levels sl
JOIN products p ON p.id = sl.product_id
JOIN warehouses w ON w.id = sl.warehouse_id
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.active = TRUE;

CREATE UNIQUE INDEX idx_mv_stock_summary ON mv_stock_summary(product_id, warehouse_id);
```

### Seed Data (v1.1)

`R__insert_dummy_category.sql` — inserts 10 sample categories for development.

Implemented as a Flyway **repeatable** migration (`R__` prefix, no version number) rather than a
versioned one, for two reasons:

1. It does not consume version V3, which stays reserved for `create_stock_tables`.
2. Repeatable migrations always run *after* all versioned migrations, so adding V3–V7 later will
   not trigger Flyway's "out of order" error.

The script is idempotent (`ON CONFLICT (id) DO NOTHING`) and calls `setval` on `categories_id_seq`
so that IDs generated by the API continue from 11.

### Stock Movement Status Flow
```
DRAFT → PENDING_APPROVAL → APPROVED → COMPLETED
                         → REJECTED
      → CANCELLED
```

## API Endpoints

### Products
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/products` | STAFF | List products (paginated, cached) |
| GET | `/api/v1/products/{id}` | STAFF | Product detail |
| GET | `/api/v1/products/search?q=` | STAFF | Full-text search |
| POST | `/api/v1/products` | MANAGER | Create product |
| PUT | `/api/v1/products/{id}` | MANAGER | Update product |

### Warehouses
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/warehouses` | STAFF | List warehouses (paginated) |
| GET | `/api/v1/warehouses/{id}` | STAFF | Warehouse detail *(added in v1.1)* |
| POST | `/api/v1/warehouses` | ADMIN | Create warehouse |
| PUT | `/api/v1/warehouses/{id}` | ADMIN | Update warehouse — `name`, `address`, `active` only; `code` is immutable *(added in v1.1)* |

### Categories *(added in v1.1)*

Required by REQ-STP-B-106 but missing from the original API table.

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/categories` | STAFF | List categories (paginated) |
| GET | `/api/v1/categories/{id}` | STAFF | Category detail |
| POST | `/api/v1/categories` | MANAGER | Create category |
| PUT | `/api/v1/categories/{id}` | MANAGER | Update category — `code` is immutable |

### Stock
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/stock` | STAFF | Stock levels (cached) |
| GET | `/api/v1/stock/summary` | STAFF | Stock summary (materialized view) |
| GET | `/api/v1/stock/low` | STAFF | Low stock products |

### Stock Movements
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/movements/import` | STAFF | Create import movement |
| POST | `/api/v1/movements/export` | STAFF | Create export movement |
| POST | `/api/v1/movements/transfer` | STAFF | Create transfer |
| GET | `/api/v1/movements` | STAFF | List movements |
| GET | `/api/v1/movements/{id}` | STAFF | Movement detail |
| PUT | `/api/v1/movements/{id}/approve` | MANAGER | Approve movement |
| PUT | `/api/v1/movements/{id}/complete` | STAFF | Complete → update stock |

### Alerts & Reorder
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/alerts` | STAFF | Active alerts |
| PUT | `/api/v1/alerts/{id}/acknowledge` | STAFF | Acknowledge alert |
| GET | `/api/v1/reorder-suggestions` | MANAGER | Reorder suggestions |
| PUT | `/api/v1/reorder-suggestions/{id}/approve` | MANAGER | Approve suggestion |

## Challenge Details

### DB: Batch Stock Update + Materialized View
- Lock stock levels in product_id order when completing a movement
- Batch update multiple stock levels in single transaction
- Materialized view `mv_stock_summary` for reporting (refresh every minute)

### Redis: Stock Level Cache + Low-Stock Alert
- **Stock Cache**: `stock:{warehouseId}:{productId}` → quantity info, TTL 5 min
- **Low-Stock Alert via Pub/Sub**: publish alert when stock <= reorderPoint

### RabbitMQ: Import → Stock Update → Reorder → Email
```
StockMovementCompletedEvent → stock.exchange
  → stock.update.queue → StockUpdateConsumer → update levels, check reorder
    → StockLowEvent → reorder.suggestion.queue → ReorderConsumer
                    → email.alert.queue → EmailConsumer (MailHog)
```

| Exchange | Type | Queue | Routing Key |
|----------|------|-------|-------------|
| stock.exchange | Topic | stock.update.queue | stock.*.completed |
| stock.exchange | Topic | reorder.suggestion.queue | stock.alert.low |
| stock.exchange | Topic | email.alert.queue | stock.alert.# |
| stock.exchange | Topic | audit.queue | stock.# |

---

## Design Decisions & Known Gaps *(v1.1)*

### Design decisions

- **Immutable business keys.** `products.sku` and `warehouses.code` cannot be changed after
  creation, so they are absent from the Update DTOs. This also means the repositories need only
  `existsByCode` / `existsBySku` — no "exists excluding self" variant.
- **Soft delete instead of hard delete.** Warehouses have no `DELETE` endpoint. A warehouse is
  retired via `PUT /{id}` with `active = false`, because `stock_levels` and `stock_movements`
  reference `warehouses(id)` — a hard delete would violate foreign keys.
- **Foreign keys are plain `Long`, not JPA relations.** `Product.categoryId` and
  `Category.parentId` are declared as `Long` rather than `@ManyToOne`, following the starter's
  existing pattern. Avoids lazy-loading surprises and JSON serialization cycles.
- **Partial update semantics.** Update DTOs use wrapper types (`Boolean`, `Integer`) and the
  mappers set `NullValuePropertyMappingStrategy.IGNORE`, so a `null` field means "leave unchanged"
  rather than "set to null".

### Known gaps (to close in later weeks)

- **Role-based authorization is not implemented.** The `Role` enum currently contains only `ADMIN`
  and `USER` — **`STAFF` and `MANAGER` do not exist**. There is no `@PreAuthorize` anywhere, and
  `SecurityConfig` only enforces `.anyRequest().authenticated()`. The **Auth** column in the API
  tables above describes the *target* model, not current behaviour. Closing this gap requires
  extending the enum and annotating the controllers.
- **Full-text search is not active yet.** `ProductServiceImpl.search()` uses a `LIKE` query
  (`findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase`). The tsvector query `searchByVector`
  exists in `ProductRepository` but is never called; `products.search_vector` has no populating
  trigger, and the GIN index lives in V6 which is not written yet.
- **`ProductService.delete()` has no endpoint.** The service method and its unit tests exist, but
  `ProductController` exposes no `DELETE` mapping — matching the API table, which lists no delete
  operation for products.

---

## Weekly Requirements

### Week 1 Requirements

#### Backend
- [ ] REQ-STP-B-101: Create `Warehouse` entity with fields: id, name, code, address, active. Add Flyway migration V2.
- [ ] REQ-STP-B-102: Create `Category` entity with fields: id, name, code, parentId (self-referencing). Add to V2 migration.
- [ ] REQ-STP-B-103: Create `Product` entity with fields: id, sku, name, description, categoryId, unit, minStock, maxStock, reorderPoint, reorderQuantity, active, searchVector. Add to V2 migration.
- [ ] REQ-STP-B-104: Implement `WarehouseService` and `CategoryService` with CRUD operations. Use MapStruct for mapping.
- [ ] REQ-STP-B-105: Implement `ProductService` with CRUD operations including full-text search by SKU/name.
- [ ] REQ-STP-B-106: Create REST endpoints for products, warehouses, and categories with pagination.
- [ ] REQ-STP-B-107: Add Flyway migrations V3 (stock_levels), V4 (stock_movements + items), V5 (alerts + reorder_suggestions), V6 (indexes), V7 (materialized view).

#### Angular
- [ ] REQ-STP-F-101: Create `ProductListComponent` with table: SKU, Name, Category, Unit, Min Stock, Reorder Point. Include pagination.
- [ ] REQ-STP-F-102: Create `ProductFormComponent` with reactive form for create/edit. Fields: sku, name, category (dropdown), unit, min/max stock, reorder point/quantity.
- [ ] REQ-STP-F-103: Create `WarehouseListComponent` showing warehouses with Name, Code, Address, Status.
- [ ] REQ-STP-F-104: Add product and warehouse routes. Wire up sidebar navigation.

#### Testing
- [ ] REQ-STP-T-101: Write unit tests for `ProductService`: create (valid + duplicate SKU), getById, update, search. Min 5 tests.
- [ ] REQ-STP-T-102: Write unit tests for `WarehouseService`: create, getAll. Min 2 tests.
- [ ] REQ-STP-T-103: Verify Swagger UI shows all endpoints correctly.

### Week 2 Requirements

#### Backend
- [ ] REQ-STP-B-201: Create `StockLevel` entity with composite unique constraint (productId + warehouseId). Implement `StockLevelService`: getByWarehouseAndProduct, getLowStock.
- [ ] REQ-STP-B-202: Create `StockMovement` and `StockMovementItem` entities. Implement `StockMovementService`: createImport, createExport, createTransfer, approve, complete.
- [ ] REQ-STP-B-203: Implement `completeMovement()` with pessimistic lock: lock stock levels by productId order, batch update quantities (add for IMPORT, subtract for EXPORT, both for TRANSFER). Validate no negative stock on export.
- [ ] REQ-STP-B-204: Create stock REST endpoints: `GET /api/v1/stock`, `GET /api/v1/stock/summary`, `GET /api/v1/stock/low`.
- [ ] REQ-STP-B-205: Create movement REST endpoints: `POST /api/v1/movements/import`, `POST /api/v1/movements/export`, `POST /api/v1/movements/transfer`, `GET /api/v1/movements`, `GET /api/v1/movements/{id}`, `PUT /api/v1/movements/{id}/approve`, `PUT /api/v1/movements/{id}/complete`.
- [ ] REQ-STP-B-206: Implement stock level cache in Redis: `stock:{warehouseId}:{productId}` with 5-min TTL. Invalidate when movement completed.
- [ ] REQ-STP-B-207: Set up RabbitMQ: `stock.exchange` (topic), queues for stock update, reorder suggestion, email alert. Create `StockEventPublisher`.
- [ ] REQ-STP-B-208: Implement `StockUpdateConsumer`: receive movement completed event, check if any product stock <= reorderPoint, publish `StockLowEvent` if so.
- [ ] REQ-STP-B-209: Implement `ReorderConsumer`: receive low stock event, create reorder suggestion. Implement `EmailConsumer`: send low-stock alert email via MailHog.

#### Angular
- [ ] REQ-STP-F-201: Create `StockLevelComponent` — table showing stock per product per warehouse: Product, Warehouse, Quantity, Reserved, Available, Status (color-coded).
- [ ] REQ-STP-F-202: Create `MovementFormComponent` — form to create import/export movement. Dynamic item rows: product (dropdown), quantity, unit cost. Add/remove items.
- [ ] REQ-STP-F-203: Create `MovementListComponent` — table of movements with: Reference, Type, Status, Warehouse, Date. Filter by type and status.
- [ ] REQ-STP-F-204: Create `MovementDetailComponent` — show movement info + items list. Approve/Complete action buttons for authorized roles.

#### Testing
- [ ] REQ-STP-T-201: Write unit tests for `StockMovementService`: createImport, complete import (stock increases), complete export (stock decreases), export with insufficient stock. Min 4 tests.
- [ ] REQ-STP-T-202: Write unit tests for stock level cache: verify cache hit after first query, verify invalidation after movement. Min 2 tests.

### Week 3 Requirements

#### Backend
- [ ] REQ-STP-B-301: Create integration test for movement flow: create import → approve → complete → verify stock levels increased.
- [ ] REQ-STP-B-302: Write concurrent export test: 5 threads complete export movements for same product simultaneously. Verify stock never goes negative.
- [ ] REQ-STP-B-303: Implement materialized view refresh: `@Scheduled(fixedDelay = 60000)` to refresh `mv_stock_summary` concurrently.
- [ ] REQ-STP-B-304: Implement low-stock alert flow: after movement completes, check reorderPoint → create `StockAlert` + `ReorderSuggestion`.
- [ ] REQ-STP-B-305: Configure RabbitMQ retry with DLQ for all queues.
- [ ] REQ-STP-B-306: Run `EXPLAIN ANALYZE` on stock queries (especially mv_stock_summary). Verify indexes.
- [ ] REQ-STP-B-307: Add complete Swagger/OpenAPI annotations.

#### Angular
- [ ] REQ-STP-F-301: Create `AlertListComponent` — active alerts with color-coded severity (red=OUT_OF_STOCK, orange=LOW_STOCK, yellow=OVERSTOCK). Acknowledge button.
- [ ] REQ-STP-F-302: Create `ReorderListComponent` — pending reorder suggestions with product info, suggested quantity, approve action.
- [ ] REQ-STP-F-303: Create `StockDashboardComponent` — summary cards: total products, low stock count, out of stock count, pending movements.

#### Testing
- [ ] REQ-STP-T-301: Write ≥ 5 integration tests using Testcontainers: product CRUD, movement lifecycle, stock level queries, alert creation.
- [ ] REQ-STP-T-302: Write concurrent test verifying stock locking prevents negative inventory.
- [ ] REQ-STP-T-303: Verify RabbitMQ retry/DLQ: failed consumer → retries → DLQ.

### Week 4 Requirements

- [ ] REQ-STP-W-401: Complete end-to-end flow: create product → import stock → stock level updated → export → low stock alert → reorder suggestion → email notification.
- [ ] REQ-STP-W-402: Cross-review Team 1 (OrderFlow) code with ≥ 10 comments.
- [ ] REQ-STP-W-403: Fix ≥ 5 bugs from mentor-created issues.
- [ ] REQ-STP-W-404: Performance test with `ab -n 1000 -c 50`. Target: avg < 200ms, error rate < 1%.
- [ ] REQ-STP-W-405: Complete README with setup guide, architecture description, API docs.
- [ ] REQ-STP-W-406: Prepare and deliver 15-20 minute demo.
