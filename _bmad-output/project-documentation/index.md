# StockPulse Existing-Project Documentation

This documentation describes the code that exists in the repository today and the implementation patterns StockPulse should preserve. It is a brownfield reference: planned behavior from `Project 4 - StockPulse.md` is not described as implemented unless matching source code exists.

## Documentation Map

- [Project overview](project-overview.md) — scope, technology, current implementation status, and major gaps.
- [Architecture](architecture.md) — runtime components, backend layers, frontend structure, and request flows.
- [Backend implementation patterns](backend-implementation-patterns.md) — CRUD layering, DTOs, MapStruct partial updates, immutable business keys, transactions, security, pagination, Redis, RabbitMQ, and Flyway.
- [Frontend implementation patterns](frontend-implementation-patterns.md) — Angular standalone components, feature services, forms, routing, authentication, pagination, and notifications.
- [API and data model](api-and-data-model.md) — currently implemented endpoints and database structures.
- [Development and testing](development-and-testing.md) — local services, configuration, commands, unit-test patterns, and integration-test infrastructure.
- [Project context](../project-context.md) — concise implementation rules loaded by BMAD-compatible workflows.

## Current Implementation Status

| Area | Status | Evidence |
|---|---|---|
| JWT authentication | Implemented | Auth controller/service, JWT provider/filter, security configuration |
| User CRUD | Implemented | Full backend CRUD, Angular list/form, unit tests |
| Product CRUD | Implemented | Backend list/detail/create/update/search, Angular list/form, unit tests |
| Warehouse backend operations | Implemented | Entity, repository, mapper, service, list/detail/create/update controller endpoints, and 8 service tests; retirement uses `active=false`, not DELETE |
| Category backend operations | Implemented | Entity, repository, mapper, service, list/detail/create/update controller endpoints, hierarchy validation, and 12 service tests; DELETE is absent |
| Warehouse/category frontend | Not implemented | No Angular routes, services, lists, or forms for either resource |
| Stock levels and movements | Not implemented | Defined only in the project requirements |
| Alerts and reorder suggestions | Not implemented | Defined only in the project requirements |
| Redis integration | Configuration only | A typed JSON `RedisTemplate<String, Object>` exists; no domain cache usage |
| RabbitMQ integration | Starter topology only | One training exchange, queue, and binding; no StockPulse publishers/consumers |
| Flyway | Implemented for V1/V2 | V2 defines warehouses, categories, and products, including warehouse/category `updated_at`; category seed rows remain inline in V2 |
| Repeatable category seed migration | Not implemented | No `R__insert_dummy_category.sql` exists in the current migration directory |
| Backend unit tests | Implemented | User, Product, Warehouse, and Category service tests |
| Concrete integration tests | Not implemented | Reusable Testcontainers base exists, but no concrete integration test extends it |

## Source-of-Truth Rule

Use the following order when documentation and implementation differ:

1. Explicit active story or user instruction.
2. Executable code, configuration, and Flyway migrations.
3. `_bmad-output/project-context.md` and this documentation set.
4. `Project 4 - StockPulse.md` for planned requirements.
5. `README.md` for starter setup guidance.

