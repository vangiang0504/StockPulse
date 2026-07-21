# API and Data Model

This reference lists currently implemented contracts only. Endpoints described solely in `Project 4 - StockPulse.md` are planned, not live.

## Common Response Shapes

Successful non-empty responses use:

```json
{
  "success": true,
  "message": "optional message",
  "data": {},
  "timestamp": "2026-01-01T00:00:00"
}
```

Paginated `data` contains:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

Error responses use the same envelope with `success: false`; validation failures put field messages in `data`.

## Authentication Endpoints

Base path: `/api/v1/auth`. These endpoints are public.

| Method | Path | Request | Response |
|---|---|---|---|
| POST | `/register` | username, email, password, fullName | accessToken, refreshToken, username, role |
| POST | `/login` | username, password | accessToken, refreshToken, username, role |
| POST | `/refresh` | refreshToken | new accessToken and refreshToken, username, role |

## User Endpoints

Base path: `/api/v1/users`. All require authentication; no role restriction is currently declared.

| Method | Path | Behavior |
|---|---|---|
| GET | `/` | Paginated/sorted User responses |
| GET | `/{id}` | User detail or 404 |
| POST | `/` | Create user; 201, duplicate username/email -> 409 |
| PUT | `/{id}` | Partial update of email/fullName/active |
| DELETE | `/{id}` | Delete; 204 |

User responses omit the password. Normal updates cannot change username, password, or role.

## Product Endpoints

Base path: `/api/v1/products`. All require authentication; no role restriction is currently declared.

| Method | Path | Behavior |
|---|---|---|
| GET | `/` | Paginated/sorted product summaries |
| GET | `/{id}` | Product detail or 404 |
| GET | `/search?q=...` | Paginated case-insensitive SKU/name substring search |
| POST | `/` | Create product; 201, duplicate SKU -> 409 |
| PUT | `/{id}` | Partial update; SKU is immutable |

`ProductService` defines delete behavior, but `ProductController` does not expose a DELETE endpoint.

## Warehouse Endpoints

Base path: `/api/v1/warehouses`. All require authentication; no role restriction is currently declared.

| Method | Path | Behavior |
|---|---|---|
| GET | `/` | Paginated/sorted warehouse responses |
| GET | `/{id}` | Warehouse detail or 404 |
| POST | `/` | Create warehouse; 201, duplicate code -> 409 |
| PUT | `/{id}` | Partial update of name/address/active; code is immutable |

There is no DELETE endpoint. A warehouse is retired by sending `active: false` in an update.

## Category Endpoints

Base path: `/api/v1/categories`. All require authentication; no role restriction is currently declared.

| Method | Path | Behavior |
|---|---|---|
| GET | `/` | Paginated/sorted category responses |
| GET | `/{id}` | Category detail or 404 |
| POST | `/` | Create category; 201, duplicate code -> 409, unknown parent -> 404 |
| PUT | `/{id}` | Partial update of name/parentId; code is immutable; invalid/circular parent -> 400 |

There is no DELETE endpoint. Category updates prevent a category from being its own parent and reject ancestry cycles.

## Pagination Query Parameters

User, Product, Warehouse, and Category lists use `page=0`, `size=20`, `sortBy=createdAt`, and `sortDir=DESC` as defaults. Product search accepts the same paging/sorting fields plus required `q`.

## Partial-Update Contract

Warehouse and Category update requests omit `code`, and their MapStruct update methods explicitly ignore both `id` and `code`. Codes are therefore immutable after creation.

All update components are nullable and the mappers use `NullValuePropertyMappingStrategy.IGNORE`. A null value means "leave the stored value unchanged." It cannot currently clear `address` or `parentId`; turning a category into a root category requires an explicit contract that does not yet exist.

## Current Database Tables

### users (V1)

- `id` BIGSERIAL primary key
- unique `username` and `email`
- BCrypt-compatible password column
- `full_name`
- role string, default `USER`
- active flag
- creation/update timestamps
- username and email indexes

### warehouses (V2)

- `id`, `name`, unique `code`, `address`, active flag, creation/update timestamps
- `Warehouse` extends `BaseEntity`; the repository supports lookup/existence by code, and the service/controller implement list, detail, create, and update.

### categories (V2)

- `id`, `name`, unique `code`, optional self-referencing `parent_id`, creation/update timestamps
- `Category` extends `BaseEntity` and stores `parentId` as a scalar `Long`; service validation enforces parent existence and prevents cycles during update.
- V2 currently seeds ten category rows and advances the identity sequence.
- No repeatable category seed migration exists. Although the v1.1 project brief describes `R__insert_dummy_category.sql`, that remains a requirement rather than executable repository state.

### products (V2)

- unique SKU and product name/description
- numeric `category_id` field in the entity; the SQL column references categories
- unit and stock threshold/reorder fields
- active flag
- PostgreSQL `TSVECTOR` search column
- creation/update timestamps

The current entity stores `categoryId` directly rather than a JPA relationship. Maintain that established ID-based mapping unless a deliberate architecture change introduces relationship objects consistently across DTOs, mapping, queries, and serialization.

## Future Database Requirements

The project requirements reserve future migrations for stock levels, movements/items, alerts/reorder suggestions, indexes, and a stock-summary materialized view. These objects are not present in the current migration directory and must not be assumed available at runtime.

