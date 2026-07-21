# Project Overview

## Purpose

The repository is a full-stack training starter being extended into StockPulse, an inventory and warehouse management system. The starter supplies authentication, error handling, infrastructure configuration, and User CRUD as the original reference implementation. Product, Warehouse, and Category backend operations are now implemented on the same layered pattern. The wider stock-movement, alerting, and reporting requirements in `Project 4 - StockPulse.md` remain future scope unless matching code exists.

## Technology Stack

### Backend

- Java 17
- Spring Boot 3.2.5
- Spring MVC and Bean Validation
- Spring Data JPA with PostgreSQL 16
- Spring Security with JJWT 0.12.5
- Flyway migrations
- MapStruct 1.5.5.Final and Lombok 1.18.32
- Spring Data Redis with Redis 7
- Spring AMQP with RabbitMQ 3
- Spring Mail with MailHog
- SpringDoc OpenAPI 2.3.0
- JUnit 5, Mockito, AssertJ, Spring Boot Test, and Testcontainers 1.19.7

### Frontend

- Angular 17.3 standalone components
- TypeScript 5.4 strict mode
- Angular Material 17.3
- RxJS 7.8
- Jasmine and Karma

## Repository Layout

```text
backend/
  pom.xml
  docker-compose.yml
  src/main/java/com/training/starter/
    common/          API response envelopes
    config/          Redis, RabbitMQ, CORS, and OpenAPI
    controller/      REST endpoints
    dto/             Request and response records
    entity/          JPA entities
    enums/           Role enum
    exception/       Domain exceptions and global handler
    mapper/          MapStruct mappings
    repository/      Spring Data repositories
    security/        JWT filter/provider and user-details adapter
    service/         Interfaces and implementations
  src/main/resources/
    application.yml
    application-test.yml
    db/migration/    Flyway SQL
  src/test/java/     Unit tests and Testcontainers base

frontend/
  src/app/
    core/            Guards, interceptor, shared API models, services
    features/        Auth, dashboard, products, and users
    layout/          Auth and main application shells
    shared/          Reusable UI components
  src/environments/  API base configuration
```

## Implemented Functional Scope

### Authentication

- Register, login, and refresh-token endpoints.
- BCrypt password hashing.
- Signed JWT access and refresh tokens.
- Stateless bearer-token authentication.
- Angular token storage, interceptor, guard, and logout flow.

### User CRUD

- Paginated/sorted list, detail, create, update, and delete endpoints.
- Username and email uniqueness checks.
- Immutable username/password/role during normal update mapping.
- Angular table, create/edit form, delete confirmation, pagination, and notifications.
- Mockito service tests for success and error paths.

### Product CRUD

- Paginated/sorted list, detail, create, update, delete at the service layer, and search.
- Unique SKU validation and separate summary/detail responses.
- Angular list and create/edit form.
- Mockito service tests.

### Warehouse Operations

- `Warehouse` entity backed by the V2 `warehouses` table, with inherited ID/audit timestamps and an `active` flag.
- Paginated/sorted list, detail, create, and partial-update endpoints under `/api/v1/warehouses`.
- Unique, immutable `code`; create rejects duplicates and update does not accept or map a code.
- Nullable update fields (`name`, `address`, `active`) are applied through a MapStruct `@MappingTarget` mapper with null-value ignore behavior.
- Retirement is represented by updating `active` to `false`; there is no warehouse DELETE endpoint.
- Eight Mockito service tests cover create, duplicate code, lookup, not-found paths, update, deactivation, and paging.

### Category Operations

- `Category` entity backed by the V2 `categories` table, with a nullable numeric `parentId` rather than a JPA relationship.
- Paginated/sorted list, detail, create, and partial-update endpoints under `/api/v1/categories`.
- Unique, immutable `code`; create validates an optional parent, and update prevents self-parenting and circular ancestry.
- Nullable update fields (`name`, `parentId`) use MapStruct null-value ignore behavior. Consequently, `parentId: null` means "leave the parent unchanged," not "make this a root category."
- There is no category DELETE endpoint.
- Twelve Mockito service tests cover success, duplicate/not-found paths, root creation, parent validation, cycle prevention, update, and paging.

## Future StockPulse Requirements Not Yet Implemented

- The current role enum is `ADMIN` and `USER`; the StockPulse brief requires `ADMIN`, `MANAGER`, and `STAFF`.
- Authentication is enforced globally, but no current controller/service method uses `@PreAuthorize` for business authorization.
- Warehouse and Category backend layers now exist, but their Angular services, routes, lists, and forms are absent.
- Warehouse retirement is available through `active=false`; category deletion and a category re-parenting operation that explicitly clears `parentId` are not implemented.
- V3 through V7 from the StockPulse brief do not exist.
- The v1.1 brief describes `R__insert_dummy_category.sql`, but the current source tree has no repeatable migration; the ten development categories are still inserted by V2.
- Redis has serialization/configuration but no cache keys, TTL policy, or domain integration.
- RabbitMQ has a generic training topology but no StockPulse event types, publisher, consumers, retry, or dead-letter queues.
- No concrete integration, concurrency, cache, or messaging tests exist yet.

