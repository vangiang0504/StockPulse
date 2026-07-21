# Backend Implementation Patterns

## CRUD Layering: Current References

User and Product remain end-to-end references that include Angular features. Warehouse and Category now provide additional backend references for immutable business keys, partial updates, soft retirement, and hierarchy validation.

### 1. Entity

`User` extends `BaseEntity`, maps to the plural `users` table, uses Lombok for accessors/building, and declares database constraints in JPA annotations. `Role` is persisted as a string rather than an ordinal.

Pattern for new entities:

- Singular PascalCase Java type and plural snake_case table.
- CamelCase Java fields with explicit snake_case column names where required.
- Keep JPA nullability, length, uniqueness, and Flyway SQL aligned.
- Extend `BaseEntity` when the table uses the standard `id`, `created_at`, and `updated_at` columns.
- Use enums with `EnumType.STRING` for durable database values.

### 2. Repository

`UserRepository` extends `JpaRepository<User, Long>` and adds semantic derived queries:

- `findByUsername`
- `findByEmail`
- `existsByUsername`
- `existsByEmail`

`WarehouseRepository` and `CategoryRepository` likewise extend `JpaRepository<..., Long>` and expose `findByCode` plus `existsByCode`. Their services use `existsByCode` on create, while update does not need an "excluding current ID" uniqueness query because code is immutable.

Use repository methods for persistence/query semantics, not business workflows. Explicit native queries are acceptable when PostgreSQL-specific behavior is required, as demonstrated by `ProductRepository.searchByVector`.

### 3. DTOs

Requests and responses are Java records in separate request/response packages.

- Create DTOs contain required fields and Bean Validation constraints.
- Update DTOs use nullable components to represent partial updates.
- Response DTOs expose only client-safe values; `UserResponse` never exposes the password hash.
- Validation messages are defined at the DTO boundary.
- Separate summary responses are used when list payloads need fewer fields (`ProductSummaryResponse`).

StockPulse should not bind controllers directly to entities.

The merged Warehouse and Category contracts make the partial-update behavior concrete:

- `CreateWarehouseRequest`: required `name` and `code`, optional `address`.
- `UpdateWarehouseRequest`: nullable `name`, `address`, and `active`; `code` is deliberately absent.
- `CreateCategoryRequest`: required `name` and `code`, optional `parentId`.
- `UpdateCategoryRequest`: nullable `name` and `parentId`; `code` is deliberately absent.
- Responses expose IDs, immutable codes, domain fields, and `createdAt`, but not `updatedAt`.

### 4. MapStruct

Mappers are interfaces annotated with `@Mapper(componentModel = "spring")`.

`UserMapper` establishes these rules:

- Convert enum role to its string name in the response.
- Ignore service-owned defaults (`role`, `active`) during creation.
- Update an existing entity with `@MappingTarget`.
- Use `NullValuePropertyMappingStrategy.IGNORE` for partial updates.
- Ignore immutable or protected fields during update (`username`, `password`, `role`).

`ProductMapper` additionally ignores generated/search fields and immutable SKU/ID values. `WarehouseMapper` and `CategoryMapper` both ignore `id` and `code` during `@MappingTarget` updates, so request payloads cannot replace their immutable business keys. `WarehouseMapper` also ignores `active` during creation, leaving the entity/database default as the owner of the initial active state.

`NullValuePropertyMappingStrategy.IGNORE` means every null update component is a no-op. This supports patch-like behavior through the PUT endpoints, but it also means clients cannot clear nullable values by sending JSON null. In particular, null cannot clear a warehouse address or turn an existing category into a root category. A future explicit clear operation would need a different request representation or service rule.

### 5. Service Interface and Implementation

The service interface exposes DTO-oriented operations. The implementation:

- Uses constructor injection via `@RequiredArgsConstructor`.
- Marks reads `@Transactional(readOnly = true)`.
- Marks writes `@Transactional`.
- Checks uniqueness before saving.
- Throws `ResourceNotFoundException` and `DuplicateResourceException` rather than returning null.
- Applies sensitive/default values in the service: User password hashing, default role, and active state.
- Maps entities to response DTOs before returning.

The current Warehouse service implements list, detail, create, and update. Duplicate codes fail with `DuplicateResourceException`; unknown IDs fail with `ResourceNotFoundException`. There is no delete method, and retirement is performed by updating `active` to false.

The current Category service implements list, detail, create, and update. It validates that an optional parent exists. During update it also rejects self-parenting and walks the parent chain to prevent circular hierarchies. It has no delete method.

Movement completion, stock locking, cache invalidation, and event publication must be coordinated in the service layer. A StockPulse write operation that updates multiple rows must have one explicit transaction boundary.

### 6. Controller

The User, Product, Warehouse, and Category controllers establish REST conventions:

- Base path `/api/v1/<plural-resource>`.
- `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, and OpenAPI `@Tag`.
- `@Valid` on request bodies.
- `@Operation` on endpoints.
- HTTP 201 for creation and 204 for delete.
- `ApiResponse<T>` for successful bodies.
- No catch blocks for expected service exceptions; the global handler owns translation.

Warehouse and Category expose GET collection, GET by ID, POST, and PUT by ID. Despite their OpenAPI tag descriptions using the word CRUD, neither controller currently exposes DELETE.

## Pagination and Sorting

User, Product, Warehouse, and Category list endpoints accept:

- `page`, default `0`
- `size`, default `20`
- `sortBy`, default `createdAt`
- `sortDir`, default `DESC`

Controllers build a Spring `Sort`, pass a `PageRequest` to the service, and convert the returned `Page` through `PageResponse.from`. The Angular paginator also uses zero-based page indices.

For new endpoints, retain the envelope and zero-based contract. The current controllers accept arbitrary sort property names; StockPulse should allow-list sortable fields to avoid runtime property errors and unintended query exposure.

## Security Conventions

- Security is stateless (`SessionCreationPolicy.STATELESS`).
- Passwords use `BCryptPasswordEncoder`.
- JWTs use an HMAC key loaded from `jwt.secret` and separate access/refresh expirations.
- `JwtAuthenticationFilter` reads a `Bearer` token, validates it, loads the user, and populates the security context.
- `UserDetailsServiceImpl` maps the stored role to `ROLE_<ROLE>` and respects the user's active flag.
- `/api/v1/auth/**`, Swagger/OpenAPI, and actuator URL patterns are public; other requests require authentication.
- `@EnableMethodSecurity` is enabled, but no current business endpoint declares role rules.

StockPulse requirements must add `STAFF` and `MANAGER` deliberately in the enum, database expectations, registration/administration flow, frontend role handling, tests, and `@PreAuthorize` rules. Frontend visibility is never a substitute for backend authorization.

## Redis Conventions and Extension Point

`RedisConfig` currently:

- Enables Spring caching.
- Defines one `RedisTemplate<String, Object>`.
- Uses `StringRedisSerializer` for keys/hash keys.
- Uses `GenericJackson2JsonRedisSerializer` for values/hash values.
- Registers Java time support and type metadata with its `ObjectMapper`.

There is no current domain cache, `CacheManager`, cache annotation usage, key TTL, or invalidation example. Therefore StockPulse should preserve the serializer/template configuration but must define its own domain cache contract:

- Key format: `stock:{warehouseId}:{productId}`.
- Five-minute TTL from the project requirements.
- Cache-aside read behavior in the stock service.
- Invalidation only after a successful committed stock change.
- Tests for cache hit, miss, expiry policy, and movement-triggered invalidation.

Do not describe cache behavior as existing until the service code and tests are present.

## RabbitMQ Conventions and Extension Point

`RabbitMQConfig` currently demonstrates the basic Spring AMQP pattern:

- Constants for exchange, queue, and routing key.
- A durable queue built with `QueueBuilder`.
- A `TopicExchange`.
- A binding built with `BindingBuilder`.
- A Jackson JSON message converter.

The configured names are `training.exchange`, `training.queue`, and `training.routing.key`. There are no publishers, listeners, retry policies, or dead-letter queues.

StockPulse should follow the same bean/configuration style while replacing the generic topology with the project-defined `stock.exchange`, stock update, reorder, email, and audit queues. Events need explicit payload types and stable identifiers. Consumers must be idempotent because RabbitMQ delivery may repeat. Configure retry and DLQ behavior rather than catching and discarding failures.

## Flyway Conventions

- Production configuration enables Flyway at `classpath:db/migration`.
- Hibernate uses `ddl-auto: validate`, so migrations are authoritative.
- Migration names use `V<sequence>__<snake_case_description>.sql`.
- V1 creates users plus lookup indexes.
- V2 creates warehouses, categories, and products. Both warehouses and categories now include `updated_at`, matching their `BaseEntity` mappings.
- V2 currently inserts ten development category rows and advances `categories_id_seq` after the explicit-ID inserts.
- SQL uses PostgreSQL-specific types and features where useful (`BIGSERIAL`, `TEXT`, `TSVECTOR`).

`Project 4 - StockPulse.md` v1.1 describes moving seed data to a repeatable `R__insert_dummy_category.sql` migration, but that file does not exist in the current source tree. The executable state is therefore the inline V2 seed. Treat the repeatable migration as not implemented until the file is present.

New schema work must be added in sequential immutable migrations. Keep entity mappings synchronized with SQL. Do not revise a migration that may already have run in a shared database; add a new corrective migration.

The test profile is intentionally different: it disables Flyway and uses `ddl-auto: create-drop`. Unit tests do not load Spring. Future migration/integration verification should use a dedicated test configuration that actually runs Flyway against PostgreSQL when migration correctness is under test.

## Logging and Error Handling

Product service methods demonstrate `@Slf4j`: debug for operation details and info for successful state changes. Follow this pattern for StockPulse workflows, while excluding tokens, passwords, and sensitive payload contents.

Use the existing exception hierarchy and `GlobalExceptionHandler`. Add domain exceptions only when an existing 400/404/409 concept cannot accurately express the failure.

