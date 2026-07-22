# AGENTS.md — StockPulse

Context and conventions for AI coding agents working in this repository.
Read this before writing any code.

## Project

StockPulse — warehouse inventory management (products, warehouses, categories, stock
movements, low-stock alerts). A 4-week, 2-developer training project (team prefix `STP`).

**`Project 4 - StockPulse.md` is the single source of truth.** It is the graded
specification: it defines the database schema, API contracts, and the `REQ-STP-*`
requirements broken down by week. Treat its **Weekly Requirements** section as the roadmap.

- Do **not** modify Weekly Requirements.
- Other sections may be corrected when they conflict with reality, but only by appending a
  new entry to the **Change Log** at the top (see v1.1 for the format).
- Do **not** generate a competing PRD or architecture document. The spec already covers both.

## Tech stack

| Layer | Stack |
|-------|-------|
| Backend | Java 17 target, Spring Boot 3.2.5, Spring Security + JWT, Spring Data JPA, MapStruct 1.5.5, Lombok, SpringDoc OpenAPI |
| Database | PostgreSQL 16 + Flyway |
| Infra | Redis 7, RabbitMQ 3, MailHog — all via Docker Compose |
| Frontend | Angular 17 (standalone components), Angular Material, RxJS |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers |

Base package: `com.training.starter`

## Local environment

Everything runs in Docker except the app itself. **The database is on port 5433, not 5432**
(`backend/docker-compose.yml` maps `5433:5432`); a native PostgreSQL may also be running on
5432 — it is unrelated to this project.

Startup order after every machine reboot:

1. Start **Docker Desktop**, wait until the engine is running.
2. `cd backend && docker compose up -d` — brings up postgres, redis, rabbitmq, mailhog.
3. If port 8080 is taken, the culprit is usually **MiniTool ShadowMaker** (`MTAgentService`),
   an unrelated backup service that claims 8080 on boot. Stop it from an **admin** shell:
   `Stop-Service MTAgentService -Force`.
4. `cd backend && ./mvnw spring-boot:run`

| Service | URL | Credentials |
|---------|-----|-------------|
| API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Angular | http://localhost:4200 | — |
| PostgreSQL | localhost:**5433** / `training_db` | postgres / postgres |
| RabbitMQ UI | http://localhost:15672 | guest / guest |
| MailHog | http://localhost:8025 | — |

## Commands

```bash
# Backend (run from backend/)
./mvnw spring-boot:run          # run the app
./mvnw test                     # unit tests
./mvnw clean verify             # full build + tests
./mvnw clean                    # REQUIRED after renaming/deleting a migration (see below)

# Frontend (run from frontend/)
npm start                       # dev server on :4200, proxies /api to :8080
npm run build
npm test

# Database
docker compose up -d            # from backend/
docker compose down -v          # DESTRUCTIVE: wipes the database volume
docker exec -it training-postgres psql -U postgres -d training_db
```

## Flyway rules — read this carefully

Migrations have caused every serious breakage in this project so far.

1. **Never edit a migration that has already been applied.** Flyway stores a checksum;
   editing an applied file makes the app refuse to start. If a change to an existing
   migration is truly required, it must be paired with `docker compose down -v` to rebuild
   the database from scratch — and every teammate must do the same.

2. **Flyway reads from the classpath, not from `src`.** The configured location is
   `classpath:db/migration`, which at runtime resolves to `backend/target/classes/db/migration/`.
   Deleting or renaming a file under `src/main/resources/db/migration/` is **not enough** —
   the stale copy in `target/classes` still runs. **Always `./mvnw clean` after touching
   migration filenames.**

3. **Version numbers are reserved by the spec.** `V1` = users, `V2` = warehouses + categories
   + products (including seed data). `V3`–`V7` are reserved for the stock tables described in
   the spec: stock levels, movements, alerts, indexes, materialized view. Do not consume those
   numbers for anything else.

4. **Seed data lives in `V2`**, appended after the `CREATE TABLE` statements. It is idempotent
   (`ON CONFLICT (id) DO NOTHING`) and calls `setval('categories_id_seq', ...)` so that
   API-generated IDs continue from 11. Do not remove the `setval` — without it, the first
   category created through the API collides with a seeded primary key.

5. **Coordinate migrations with the other developer before writing them.** Two people editing
   the same migration file is the most likely source of a painful merge.

## Backend conventions

`ddl-auto: validate` is enabled — Hibernate verifies every entity against the real schema at
startup. A mapping that does not match the database is a startup failure, not a warning.

### Layering

Follow the existing vertical slice, in this order:

```
Entity → Repository → DTOs (request + response) → MapStruct Mapper
      → Service (interface) → ServiceImpl → Controller → Unit tests
```

Reference implementations: `Warehouse*` (simple CRUD) and `Category*` (CRUD plus
self-referencing hierarchy validation).

### Entities

- Extend `BaseEntity`, which supplies `id`, `createdAt`, `updatedAt` and the
  `@PrePersist` / `@PreUpdate` hooks.
- **Any table backing a `BaseEntity` subclass must have both `created_at` and `updated_at`.**
  Omitting `updated_at` produces `Schema-validation: missing column [updated_at]` at startup.
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- `@Builder` on a subclass does **not** expose inherited fields — `id` is set via
  `setId(...)`, typically only in tests.
- Foreign keys are plain `Long` fields (`Product.categoryId`, `Category.parentId`), **not**
  `@ManyToOne` relations. This avoids lazy-loading surprises and JSON serialization cycles.

### DTOs

Java `record`s, split across `dto/request` and `dto/response`.

- **Create request** — `@NotBlank` / `@NotNull` on required fields, `@Size` / `@Min` for bounds.
  Omits `active` so the entity's `@Builder.Default` applies.
- **Update request** — validation only (`@Size`, `@Min`), never `@NotNull`. Uses **wrapper
  types** (`Boolean`, `Integer`) so `null` means "leave unchanged".
- **Business keys are immutable** and therefore absent from update requests: `Product.sku`,
  `Warehouse.code`, `Category.code`.
- **Response** — flat record ending with `createdAt`.

### Mappers (MapStruct)

`@Mapper(componentModel = "spring")`. The update method must carry:

```java
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Mapping(target = "id", ignore = true)
@Mapping(target = "code", ignore = true)   // the immutable business key
void updateEntity(@MappingTarget Warehouse warehouse, UpdateWarehouseRequest request);
```

`IGNORE` is what makes partial updates safe — without it a `PUT` that only changes `name`
nulls out every other column. Implementations are generated at build time into
`target/generated-sources/annotations/`; run `./mvnw compile` if the IDE cannot resolve
`*MapperImpl`.

### Services

Interface in `service/`, implementation in `service/impl/`, annotated
`@Service @RequiredArgsConstructor @Slf4j`. Reads use `@Transactional(readOnly = true)`,
writes use `@Transactional`.

Validate before mutating, and map failures to the right exception:

| Situation | Exception | HTTP |
|-----------|-----------|------|
| Missing by id | `ResourceNotFoundException("Warehouse", id)` | 404 |
| Duplicate business key (check `existsByCode` first) | `DuplicateResourceException("Warehouse", "code", value)` | 409 |
| Invalid input / broken invariant | `BadRequestException(message)` | 400 |

Checking `existsByX` before insert matters: letting the database unique constraint fire
surfaces as a 500 instead of a clean 409.

**Deletion is soft.** Entities carrying an `active` flag are retired via
`PUT /{id}` with `active = false`. Stock tables reference `warehouses(id)`, so a hard delete
would violate foreign keys. Do not add `DELETE` endpoints without discussing it first.

**Self-referencing hierarchies** (`Category.parentId`) must reject a node being its own
parent and must walk the ancestor chain to reject cycles — see
`CategoryServiceImpl.validateParent`. A cycle makes any tree traversal loop forever.

### Controllers

```java
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "...")
```

- Every response is wrapped in `ApiResponse.success(...)`.
- Lists return `ApiResponse.success(PageResponse.from(result, r -> r))`.
- Pagination parameters are `page`, `size`, `sortBy`, `sortDir` with defaults
  `0`, `20`, `createdAt`, `DESC`.
- `@ResponseStatus(HttpStatus.CREATED)` on `POST`.
- Annotate every operation with `@Operation(summary = "...")` for Swagger.

## Testing

Unit tests are Mockito-based — mock the repository and the mapper, inject the `*ServiceImpl`:

```java
@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private WarehouseMapper warehouseMapper;
    @InjectMocks private WarehouseServiceImpl warehouseService;
}
```

- Method naming: `method_condition_expectedResult`
  (e.g. `create_duplicateCode_throwsDuplicateResourceException`).
- Structure bodies with `// Given` / `// When` / `// Then`.
- Cover the happy path plus every guard clause; assert with AssertJ.
- Mockito runs in strict mode — do not stub calls a test does not make.
- The suite must stay green: `./mvnw test`.

## Frontend conventions

- Angular 17 standalone components; routes are lazy-loaded via `loadComponent`.
- Feature folders under `src/app/features/<feature>/` containing `*-list`, `*-form`,
  `<feature>.model.ts`, `<feature>.service.ts`.
- Services call the relative base `environment.apiUrl` (`/api/v1`); `proxy.conf.json`
  forwards to `http://localhost:8080`. **If the backend port changes, update the proxy too.**
- Templates and styles are inline in the component decorator (existing style).
- Add new pages to `app.routes.ts` *and* the sidebar in
  `layout/main-layout/main-layout.component.ts`.

## Git conventions

- Commit messages are prefixed with the author handle: `khoa-nxd: ...`, `giang-hv: ...`.
- Never commit build output. `backend/target/`, `node_modules/`, `.angular/` and `dist/` are
  in `.gitignore`; committed build artifacts previously caused stale migrations to execute.
- Keep the working tree clean before pulling — this repo has already lost work to a partial
  commit followed by a merge.
- Run `./mvnw test` before pushing.

## Known gaps

Do not assume these work; they are documented in the spec's *Known Gaps* section.

- **Role-based authorization does not exist.** The `Role` enum has only `ADMIN` and `USER` —
  `STAFF` and `MANAGER` from the spec's API tables are not implemented, and there is no
  `@PreAuthorize` anywhere. `SecurityConfig` enforces only `.anyRequest().authenticated()`,
  so any logged-in user can call any endpoint.
- **Full-text search is not active.** `ProductServiceImpl.search()` uses a `LIKE` query.
  The `searchByVector` tsvector query exists in `ProductRepository` but is never called,
  `products.search_vector` has no populating trigger, and the GIN index belongs to the
  not-yet-written `V6`.
- **`ProductService.delete()` has no endpoint** — the service method and its tests exist, but
  `ProductController` exposes no `DELETE` mapping.

## Hard rules

1. Do not edit the Weekly Requirements in `Project 4 - StockPulse.md`.
2. Do not edit an already-applied Flyway migration without a full database reset.
3. Do not consume migration versions `V3`–`V7`; they are reserved by the spec.
4. Run `./mvnw clean` after any migration file is renamed or deleted.
5. Do not commit `backend/target/` or any other build output.
6. Do not add `DELETE` endpoints — retire records with `active = false`.
7. Do not introduce `@ManyToOne` relations; keep foreign keys as `Long`.
