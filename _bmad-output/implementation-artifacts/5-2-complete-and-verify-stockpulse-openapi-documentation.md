# Story 5.2: Complete and Verify Week 1 StockPulse OpenAPI Documentation

Status: ready-for-dev

## Story

As a StockPulse API reviewer or client developer,
I want the generated OpenAPI document and Swagger UI to describe every implemented Week 1 StockPulse endpoint accurately,
so that I can discover and verify the delivered catalog and warehouse API without being shown unavailable Week 2–4 functionality.

## Scope

This is the Week 1 portion of Story 5.2 for `REQ-STP-T-103`. It covers only the currently implemented Product, Category, and Warehouse controller operations. It does not complete the later cross-project portion of Story 5.2 or claim completion of `REQ-STP-B-307` for future StockPulse APIs.

The 13 operations in scope are:

| Tag | Method and path | Success | Authorized roles |
|---|---|---:|---|
| Products | `GET /api/v1/products` | 200 | STAFF, MANAGER, ADMIN |
| Products | `GET /api/v1/products/{id}` | 200 | STAFF, MANAGER, ADMIN |
| Products | `GET /api/v1/products/search` | 200 | STAFF, MANAGER, ADMIN |
| Products | `POST /api/v1/products` | 201 | MANAGER, ADMIN |
| Products | `PUT /api/v1/products/{id}` | 200 | MANAGER, ADMIN |
| Categories | `GET /api/v1/categories` | 200 | STAFF, MANAGER, ADMIN |
| Categories | `GET /api/v1/categories/{id}` | 200 | STAFF, MANAGER, ADMIN |
| Categories | `POST /api/v1/categories` | 201 | MANAGER, ADMIN |
| Categories | `PUT /api/v1/categories/{id}` | 200 | MANAGER, ADMIN |
| Warehouses | `GET /api/v1/warehouses` | 200 | STAFF, MANAGER, ADMIN |
| Warehouses | `GET /api/v1/warehouses/{id}` | 200 | STAFF, MANAGER, ADMIN |
| Warehouses | `POST /api/v1/warehouses` | 201 | ADMIN |
| Warehouses | `PUT /api/v1/warehouses/{id}` | 200 | ADMIN |

Starter Auth and User endpoints may remain in the generated document, but they are baseline reference APIs and are not annotation or assertion targets of this Week 1 story.

## Acceptance Criteria

1. The generated OpenAPI JSON contains exactly the 13 implemented Week 1 StockPulse method/path pairs listed above, under the existing Products, Categories, and Warehouses tags. Each operation has a unique operation ID, a useful summary and description, and the correct success status (200 or 201).
2. Every in-scope operation declares JWT bearer security through the existing `Bearer Authentication` HTTP bearer scheme and states its actual allowed roles in the operation description. Documentation matches the current `@PreAuthorize` expressions and does not change authorization behavior.
3. List operations document the zero-based `page` parameter (default 0, minimum 0), `size` (default 20, minimum 1), `sortBy` (default `createdAt`), and `sortDir` (default `DESC`, values `ASC`/`DESC`). Product search additionally documents required, non-blank `q`. No parameter restriction is claimed unless current controller/service behavior enforces it.
4. Create and update request schemas expose the existing Bean Validation contract, including required fields, string lengths, and numeric minima. Descriptions also identify immutable fields: Product SKU, Category code, and Warehouse code are not update properties. No request field or constraint is invented.
5. Success responses describe the existing `ApiResponse<T>` envelope (`success`, optional `message`, `data`, `timestamp`), the concrete Product/Category/Warehouse response DTO, and, for list/search operations, the nested `PageResponse<T>` metadata (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`).
6. Applicable failure responses are documented with the existing error-envelope shape and status semantics: 400 for validation/bad query/data-integrity failures, 401 for missing or invalid authentication, 403 for insufficient roles, 404 for missing resources, and 409 for duplicate Product SKU, Category code, or Warehouse code. Responses are attached only where that operation can produce them; no blanket 404/409 is added to every operation.
7. An automated OpenAPI JSON test obtains `/v3/api-docs` from a focused Spring test context and asserts the complete Week 1 path/operation allowlist, success and applicable error status codes, bearer security, required parameters/defaults/constraints, request schemas, response-envelope/page schemas, and the three StockPulse tags.
8. The automated test also asserts that unimplemented operations are absent: no Product, Category, or Warehouse `DELETE`; no `/api/v1/stock`, `/api/v1/stock/**`, `/api/v1/movements`, `/api/v1/movements/**`, `/api/v1/alerts`, `/api/v1/alerts/**`, `/api/v1/reorder-suggestions`, `/api/v1/reorder-suggestions/**`, dashboard/reporting, or other Week 2–4 domain path is exposed as available.
9. A repeatable Swagger UI smoke check is executed against `/swagger-ui.html`, and its result is recorded in this story's Dev Agent Record. It confirms tag grouping, all 13 method/path pairs, parameter defaults, request schemas, response statuses/envelopes, and the Authorize control. It also searches for and confirms the absence of the future paths from AC 8.
10. The implementation changes only OpenAPI annotations/configuration and focused OpenAPI verification coverage. It does not add controller mappings, domain behavior, DTO fields, validation rules, authorization rules, database changes, frontend changes, or Week 2–4 documentation.

## Tasks / Subtasks

- [ ] Capture and lock the generated Week 1 baseline (AC: 1, 7, 8)
  - [ ] Start the backend using the established test/local infrastructure and save or inspect the generated `/v3/api-docs` response before editing annotations.
  - [ ] Confirm the generated document includes the 13 in-scope method/path pairs and record any unexpected StockPulse path before proceeding.
  - [ ] Treat controller mappings as the availability authority. Do not add paths directly to `OpenApiConfig` to represent planned endpoints.
  - [ ] Keep Auth/User paths outside the Week 1 allowlist comparison; do not remove or refactor those baseline APIs in this story.
- [ ] Complete operation, security, and parameter annotations (AC: 1-3, 10)
  - [ ] Extend the existing `@Operation` annotations in `ProductController`, `CategoryController`, and `WarehouseController` with stable operation IDs, meaningful descriptions, and actual role requirements.
  - [ ] Add explicit `@SecurityRequirement(name = "Bearer Authentication")` at controller or operation level for the 13 protected operations. Preserve the existing scheme name so annotations and `OpenApiConfig` cannot drift.
  - [ ] Add `@Parameter`/`@Schema` metadata for `id`, list pagination/sort parameters, and product-search `q`, including defaults, requiredness, examples, and only the constraints enforced by the current code.
  - [ ] Ensure operation ordering/path matching does not confuse `/products/search` with `/products/{id}` in the generated document.
- [ ] Complete request and response documentation (AC: 4-6, 10)
  - [ ] Retain Bean Validation as the source of request-schema required fields and limits; add `@Schema` descriptions/examples only where the generated contract is unclear.
  - [ ] Document concrete success content for every operation and preserve the actual 201 status on the three create operations.
  - [ ] Reuse a small annotation pattern or shared documented error schema where it reduces duplication without changing runtime `ApiResponse` serialization.
  - [ ] Add 400/401/403/404/409 `@ApiResponse` entries according to the operation-specific matrix below.
  - [ ] Verify generic success schemas resolve to the correct concrete `data` item and that paged results expose both `content` item type and all six page metadata properties.
- [ ] Add focused generated-JSON assertions (AC: 1-8, 10)
  - [ ] Create `Week1OpenApiDocumentationTest` under the backend controller/config test area using MockMvc and a focused Spring MVC/SpringDoc context; mock services and unrelated infrastructure rather than requiring PostgreSQL, Redis, or RabbitMQ merely to generate documentation.
  - [ ] Parse `/v3/api-docs` with the existing Jackson test support and use table-driven assertions for the 13-operation allowlist and per-operation success/error expectations.
  - [ ] Assert `components.securitySchemes["Bearer Authentication"]` has `type: http`, `scheme: bearer`, and `bearerFormat: JWT`, and every in-scope operation has that security requirement.
  - [ ] Assert list/search query parameter names, locations, required flags, defaults, enum/minimum values, and request-body requiredness/schema references.
  - [ ] Resolve `$ref` values in helpers instead of coupling assertions to SpringDoc's generated generic-wrapper component names; still assert the concrete DTO fields and envelope/page structure.
  - [ ] Add negative path and operation assertions from AC 8 so later annotations cannot accidentally advertise controller methods that do not exist.
- [ ] Perform and record the Swagger UI smoke verification (AC: 9)
  - [ ] Run the backend with its normal OpenAPI settings and open `/swagger-ui.html` (accepting the normal redirect to `/swagger-ui/index.html`).
  - [ ] Confirm Products shows 5 operations, Categories 4, and Warehouses 4, with the exact methods and paths in this story.
  - [ ] Expand representative list, detail, create, update, and search operations and check descriptions, roles, parameters, request constraints, success/error responses, and concrete schemas.
  - [ ] Confirm the Authorize dialog accepts a bearer token and protected operations show the bearer security requirement; this is a documentation smoke check, not a replacement for authorization tests.
  - [ ] Search the rendered UI for the future resource names and paths in AC 8 and record that they are absent.
  - [ ] Record date, environment/profile, generated-doc URL, UI URL, result, and any limitations in `Swagger UI Smoke Verification` under the Dev Agent Record.
- [ ] Run proportional verification (AC: 1-10)
  - [ ] Run the focused OpenAPI documentation test and the existing affected backend tests.
  - [ ] Regenerate `/v3/api-docs` after changes and compare the Week 1 operation set with the allowlist.
  - [ ] Review the final diff to confirm there are no new mappings, future endpoint annotations, runtime behavior changes, or Week 2–4 availability claims.

## Operation-Specific Response Matrix

All 13 operations document 401 and 403 in addition to their success response.

| Operation group | Additional documented failures |
|---|---|
| Product list | 400 for invalid pagination or sort input |
| Product search | 400 for blank `q`, invalid pagination/direction, or unsupported search sort property |
| Product detail | 404 when the Product does not exist |
| Product create | 400 for request validation or invalid related data; 409 for duplicate SKU |
| Product update | 400 for request validation or invalid related data; 404 when the Product does not exist |
| Category list | 400 for invalid pagination or sort input |
| Category detail | 404 when the Category does not exist |
| Category create | 400 for request validation; 404 when `parentId` does not exist; 409 for duplicate code |
| Category update | 400 for request validation, self-parenting, or a circular hierarchy; 404 when the Category or requested parent does not exist |
| Warehouse list | 400 for invalid pagination or sort input |
| Warehouse detail | 404 when the Warehouse does not exist |
| Warehouse create | 400 for request validation; 409 for duplicate code |
| Warehouse update | 400 for request validation; 404 when the Warehouse does not exist |

Do not document 409 on update operations while their immutable unique identifiers are not accepted by update DTOs. Do not document Product delete merely because `ProductService.delete` exists; there is no Product delete controller mapping.

## Dev Notes

### Inspected current implementation

- `ProductController` exposes five operations: paginated list, detail, search, create, and update. `ProductService.delete()` is not exposed and must remain absent from OpenAPI.
- `CategoryController` and `WarehouseController` each expose four operations: paginated list, detail, create, and update. Neither exposes delete.
- The controllers already have `@Tag` and summary-only `@Operation` annotations. They do not explicitly describe parameter semantics, role requirements, request/response content, or operation-specific error responses.
- `@PreAuthorize` supplies the actual role matrix but SpringDoc does not turn those expressions into human-readable role documentation. Add role descriptions without changing the expressions.
- `OpenApiConfig` currently defines a global `Bearer Authentication` HTTP bearer/JWT scheme and applies it globally. The story must at least prove explicit security on every in-scope operation; it must not broaden into an Auth/User documentation cleanup.
- SpringDoc derives request constraints from Jakarta Bean Validation annotations on the create/update records. Preserve those constraints rather than duplicating inconsistent limits in prose.
- `ApiResponse<T>` serializes `success`, optional `message`, `data`, and `timestamp`; `PageResponse<T>` serializes `content`, `page`, `size`, `totalElements`, `totalPages`, and `last`.
- `GlobalExceptionHandler` returns the same `ApiResponse` family for domain, validation, authentication, authorization, and integrity failures. Security-filter 401/403 responses also use the project's REST security handler and must be represented consistently.
- A live fetch of `http://localhost:8080/v3/api-docs` during story creation failed because no backend was listening on port 8080. Therefore the first implementation task explicitly captures the generated baseline before annotations change; the source inspection establishes the expected current gap but is not recorded as a passed runtime verification.

### Annotation guidance

- Prefer `io.swagger.v3.oas.annotations` on the existing controller methods and DTO record components. Do not create a hand-maintained OpenAPI path tree that can diverge from Spring MVC mappings.
- Use stable, domain-specific operation IDs such as `listProducts`, `getProduct`, `searchProducts`, `createProduct`, and `updateProduct`, with equivalent Category/Warehouse names.
- Role text should say `STAFF, MANAGER, or ADMIN`, `MANAGER or ADMIN`, or `ADMIN only` exactly as implemented. OpenAPI bearer security expresses authentication, while descriptions express role authorization.
- Keep schema examples plausible but generic. Do not embed credentials, production hosts, or database IDs presented as guaranteed seed data.
- `sortBy` behavior is not uniformly constrained across all list endpoints. Document the current default and purpose; add an enum only where the service currently enforces a finite allowlist.
- Keep `id` documented as a required integer path parameter. Do not claim a positive minimum unless runtime validation is also present.
- A 400 response may cover malformed pagination/direction input and request validation. Documentation must not promise a field-error map for every 400 because domain `BadRequestException` responses use a string message without validation data.

### Automated assertion design

- Store the expected operation contract as test arguments containing path, HTTP method, success code, allowed-role text, parameter set, request schema when present, and extra error codes. This keeps the test reviewable and prevents 13 copy-pasted blocks.
- Compare only StockPulse paths against the exact Week 1 allowlist. The application legitimately includes starter Auth/User paths that are outside this story.
- Assert both presence and absence. Presence protects `REQ-STP-T-103`; absence protects reviewers from mistaking database migrations, service methods, or future requirements for available HTTP APIs.
- For response schema checks, follow `$ref` recursively and inspect properties rather than relying on incidental component naming for nested generics.
- The generated document assertion is the repeatable gate. The Swagger UI check validates rendering/discoverability and must leave a compact evidence record, but it need not automate a browser in this Week 1 story.

### Expected affected files

- `backend/src/main/java/com/training/starter/controller/ProductController.java`
- `backend/src/main/java/com/training/starter/controller/CategoryController.java`
- `backend/src/main/java/com/training/starter/controller/WarehouseController.java`
- `backend/src/main/java/com/training/starter/config/OpenApiConfig.java` (only if shared description/schema support is required)
- `backend/src/main/java/com/training/starter/dto/request/CreateProductRequest.java` (only missing schema descriptions/examples)
- `backend/src/main/java/com/training/starter/dto/request/UpdateProductRequest.java` (only missing schema descriptions/examples)
- `backend/src/main/java/com/training/starter/dto/request/CreateCategoryRequest.java` (only missing schema descriptions/examples)
- `backend/src/main/java/com/training/starter/dto/request/UpdateCategoryRequest.java` (only missing schema descriptions/examples)
- `backend/src/main/java/com/training/starter/dto/request/CreateWarehouseRequest.java` (only missing schema descriptions/examples)
- `backend/src/main/java/com/training/starter/dto/request/UpdateWarehouseRequest.java` (only missing schema descriptions/examples)
- `backend/src/test/java/com/training/starter/controller/Week1OpenApiDocumentationTest.java` (new; exact package may be `config` if it better matches the focused context)

Explicitly do not add or annotate mappings for StockLevel, StockMovement, Alert, ReorderSuggestion, Dashboard/reporting, Product delete, Category delete, or Warehouse delete. Do not modify services, repositories, entities, migrations, frontend code, or runtime authorization/validation as part of this documentation story.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 5, Story 5.2 and complete requirement traceability]
- [Source: `_bmad-output/project-context.md` — Week 1 implementation status and technology baseline]
- [Source: `Project 4 - StockPulse.md` — REQ-STP-T-103 and Week 1–4 endpoint requirements]
- [Source: `task.md` — Week 1 remaining Swagger/OpenAPI work]
- [Source: current `ProductController.java`, `CategoryController.java`, and `WarehouseController.java` — implemented mappings, roles, status codes, and existing annotations]
- [Source: current request/response DTOs, `ApiResponse.java`, and `PageResponse.java` — validation and schema contract]
- [Source: current `OpenApiConfig.java`, `SecurityConfig.java`, `RestSecurityErrorHandler.java`, and `GlobalExceptionHandler.java` — bearer scheme and error behavior]
- [Source: `backend/src/main/resources/application.yml` — `/v3/api-docs` and `/swagger-ui.html` configuration]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### Swagger UI Smoke Verification

- Date/time:
- Environment/profile:
- OpenAPI JSON URL:
- Swagger UI URL:
- Week 1 operation count and result:
- Security/parameter/schema result:
- Future-path absence result:
- Overall result:
- Notes/limitations:

### File List
