# Story 1.2: Activate PostgreSQL Product Full-Text Search

Status: ready-for-dev

## Story

As a StockPulse staff member,
I want the existing Product search endpoint to use PostgreSQL full-text search for SKU and name,
so that I can find catalog items efficiently without changing the Product CRUD workflow.

## Acceptance Criteria

1. `ProductRepository.searchByVector` uses `plainto_tsquery('simple', :query)` in both its result query and count query, matching the `simple` text-search configuration established by V6. Search input remains a bound parameter and is never concatenated into native SQL.
2. `ProductServiceImpl.search` normalizes valid input and delegates to `ProductRepository.searchByVector`; the legacy `findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase` method is no longer used by the runtime search path.
3. Null, empty, and whitespace-only search input is rejected with `BadRequestException`, producing HTTP 400 through the existing `GlobalExceptionHandler` error envelope without calling the repository.
4. `GET /api/v1/products/search` retains its existing path, authorization, request parameters, zero-based pagination, sorting behavior, and `ApiResponse<PageResponse<ProductSummaryResponse>>` response contract.
5. PostgreSQL integration coverage proves case-insensitive SKU/name term matching, safe handling of plain-text special characters, no-match behavior, and correct pagination/count metadata through `searchByVector`.
6. Product CRUD, DTOs, mappers, frontend code, and Flyway migrations are unchanged. V6 backfill, trigger maintenance, `search_vector NOT NULL`, and the GIN index are accepted database foundation from Story 2.1 and are not recreated or modified here.

## Tasks / Subtasks

- [ ] Align the existing vector query with V6 (AC: 1)
  - [ ] Change both `plainto_tsquery(:query)` calls in `ProductRepository.searchByVector` to `plainto_tsquery('simple', :query)`.
  - [ ] Keep `:query` parameter-bound in both result and count SQL.
  - [ ] Do not edit `V6__create_indexes.sql` or add another migration.
- [ ] Activate the vector search path in the existing service (AC: 2, 3)
  - [ ] Normalize search input by trimming it and collapsing repeated whitespace before repository delegation.
  - [ ] Throw the existing `BadRequestException` for null, empty, or whitespace-only input.
  - [ ] Replace the substring-query delegation with one call to `searchByVector`, then preserve the existing `ProductMapper.toSummaryResponse` page mapping.
  - [ ] Remove `findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase` only if no caller remains.
- [ ] Preserve the existing endpoint contract (AC: 3, 4)
  - [ ] Keep `ProductController.search`, its route, parameters/defaults, STAFF/MANAGER/ADMIN authorization, and response wrapping unchanged.
  - [ ] Verify that the native query works with the endpoint's current default `createdAt DESC` sort and zero-based `PageRequest`.
  - [ ] If native-query sorting requires translation, use a fixed allow-list from supported Product sort properties to SQL column names; never interpolate arbitrary client text into native SQL.
- [ ] Update only the affected tests (AC: 2-5)
  - [ ] Update `ProductServiceTest.search_validQuery_returnsPageOfProducts` to verify normalized input is passed to `searchByVector` and results are mapped.
  - [ ] Add null/empty/whitespace cases that expect `BadRequestException` and verify no search repository call.
  - [ ] Add focused API-level coverage showing blank `q` returns HTTP 400 with the established error envelope.
  - [ ] Add a focused PostgreSQL 16 integration test for SKU/name matching, case insensitivity, special-character safety, no match, default sorting, multiple pages, and count metadata.
  - [ ] Reuse production V6 through Flyway in the integration test; do not duplicate its SQL or repeat Story 2.1 migration tests.

## Dev Notes

### Inspected current implementation

- `ProductRepository.searchByVector` already exists as a paginated native query over `products.search_vector`, but it currently calls `plainto_tsquery(:query)` without an explicit configuration in both the value and count query.
- V6 builds `search_vector` with `to_tsvector('simple', ...)`. PostgreSQL uses the database/session default when `plainto_tsquery` omits its configuration, so the repository must explicitly use `'simple'` to guarantee the vector and query configurations match.
- `ProductServiceImpl.search` currently bypasses `searchByVector` and calls `findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(query, query, pageable)`. Switching this delegation is the core activation work.
- `ProductController.search` already provides the required endpoint, pagination envelope, and authorization. It should not be redesigned.
- `Product.searchVector` is already read-only to JPA with `insertable = false, updatable = false`; PostgreSQL remains the owner of this value.

### Input and query rules

- Use `BadRequestException` because `GlobalExceptionHandler` already maps it to HTTP 400 with `ApiResponse.error(...)`.
- Normalize with trim plus internal-whitespace collapse. Pass the normalized string as the repository parameter.
- `plainto_tsquery` treats input as plain text rather than accepting raw tsquery operators. Together with parameter binding, this safely handles quotes and punctuation without manual SQL escaping.
- Full-text search matches lexemes/terms, not arbitrary substrings. Tests must not preserve the legacy `%substring%` behavior accidentally.
- V6 weights SKU as `A` and name as `B`, but this story does not add relevance ranking. Preserve the existing explicit sort contract.

### Scope guardrails

- Do not recreate or modify Product CRUD, Product DTOs, MapStruct mappings, controllers other than test coverage, frontend services/components, security roles, or response envelopes.
- Do not edit V1-V7, add V8, duplicate the V6 trigger/index logic, or add migration verification already owned by Story 2.1.
- Do not introduce Elasticsearch, Hibernate Search, a second search endpoint, raw `to_tsquery` parsing, or user-built SQL.
- Keep the existing controller -> service -> repository layering and `@Transactional(readOnly = true)` service boundary.

### Testing guidance

- Follow the existing JUnit 5, Mockito, AssertJ, and Given/When/Then conventions in `ProductServiceTest`.
- Use PostgreSQL rather than H2 for repository integration because `tsvector` and `plainto_tsquery` are PostgreSQL-specific.
- Run the focused integration test with production Flyway migrations and `ddl-auto=validate`; the normal `test` profile disables Flyway and therefore is not suitable without explicit overrides.
- Use deterministic records and enough matches to cross a page boundary before asserting page membership and totals.
- Existing `FlywayMigrationIntegrationTest` owns V6 backfill, trigger, index, and migration validation. Do not reproduce those cases.

### Expected affected files

- `backend/src/main/java/com/training/starter/repository/ProductRepository.java`
- `backend/src/main/java/com/training/starter/service/impl/ProductServiceImpl.java`
- `backend/src/test/java/com/training/starter/service/ProductServiceTest.java`
- `backend/src/test/java/com/training/starter/repository/ProductSearchIntegrationTest.java` (new)
- `backend/src/test/java/com/training/starter/controller/ProductControllerTest.java` (new, or equivalent focused API test)

Explicitly do not modify Product CRUD files, frontend files, or `backend/src/main/resources/db/migration/V6__create_indexes.sql`.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.2]
- [Source: `task.md` — REQ-STP-B-105 remaining application work]
- [Source: current `ProductRepository.searchByVector` — native value/count queries omit `'simple'`]
- [Source: current `ProductServiceImpl.search` — legacy substring repository delegation]
- [Source: current `ProductController.search` — existing endpoint, pagination, authorization, and response contract]
- [Source: current `V6__create_indexes.sql` and `FlywayMigrationIntegrationTest` — accepted Story 2.1 database foundation]
- [Source: `_bmad-output/project-documentation/backend-implementation-patterns.md` and `development-and-testing.md`]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

