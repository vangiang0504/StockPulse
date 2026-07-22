# Story 1.4: Replace Product Category ID Entry with a Dropdown

Status: ready-for-dev

## Story

As a StockPulse manager,
I want to choose a Product category from a named dropdown,
so that I can create or edit Products without knowing internal Category IDs.

## Acceptance Criteria

1. The existing numeric `Category ID` input in `ProductFormComponent` is replaced with an Angular Material select whose options display Category names (with codes available to disambiguate duplicate-looking names) and whose form value remains the numeric Category ID.
2. Category options are loaded through a typed frontend Category service from the existing zero-based `GET /api/v1/categories` paginated endpoint and its `ApiResponse<PageResponse<CategoryResponse>>` envelope; the implementation follows pagination metadata until all available pages have been accumulated and does not assume the default or first page contains every Category.
3. In edit mode, Product detail and Category option loading are coordinated so the Product's existing `categoryId` is selected only after its matching option is available. A Product whose Category appears on any later Category API page is visibly preselected without changing the Product contract or issuing an update.
4. The select keeps `categoryId` required and create/update requests send the selected numeric ID through the existing `CreateProductRequest` or `UpdateProductRequest`. All other Product fields, validation/defaults, create/edit branching, disabled SKU behavior, notifications, and navigation remain unchanged.
5. While Category options are loading, the select communicates loading and submission is unavailable. If the paginated request fails, returns an unsuccessful/missing-data envelope, or yields no Categories, the form shows an explicit error or empty state, reports a stable notification where appropriate, and cannot be submitted with unavailable reference data.
6. Category pages are requested in a deterministic order (for example `name,ASC`) with a bounded page size, accumulated without duplicate IDs, and traversal stops from server pagination metadata (`last` or `totalPages`) rather than a hard-coded page count.
7. Focused Angular coverage verifies the typed service contract, first-page and multi-page loading, later-page edit preselection, required selection and numeric payload ID, Category loading/empty/error states, and preservation of existing create/edit behavior.

## Tasks / Subtasks

- [ ] Add a typed frontend Category API boundary (AC: 2, 6)
  - [ ] Create a Category model matching the existing backend response fields: `id`, `name`, `code`, nullable `parentId`, and `createdAt`.
  - [ ] Add a root-provided Category service under `features/categories` that builds its URL from `environment.apiUrl` and exposes a typed paginated list call with zero-based `page`, `size`, `sortBy`, and `sortDir` parameters.
  - [ ] Return `Observable<ApiResponse<PageResponse<Category>>>`; do not use `any`, unwrap raw arrays, duplicate JWT headers, or change the backend Category API.
  - [ ] Keep multi-page orchestration in one clearly testable place (service helper or Product form), requesting subsequent pages from the returned pagination metadata and merging by Category ID.
- [ ] Replace only the Product form's Category control UI (AC: 1, 4)
  - [ ] Import `MatSelectModule` into the existing standalone `ProductFormComponent`.
  - [ ] Replace the number input with a `mat-select` bound to the existing `categoryId` control and render options with numeric `[value]="category.id"`.
  - [ ] Display a business-readable option label such as `Name (CODE)` while preserving the submitted request property and numeric value.
  - [ ] Preserve the rest of the inline form, validators, defaults, SKU edit disabling, request construction, notifications, and routes.
- [ ] Load every Category page safely (AC: 2, 5, 6)
  - [ ] Begin at page `0` with a bounded size such as `50` and a deterministic supported sort such as `name,ASC`.
  - [ ] Accumulate page content until the response reports the last page (or the next page reaches `totalPages`); do not request a fixed set of pages or use `totalElements` as a page index.
  - [ ] De-duplicate accumulated options by `id` and retain deterministic display ordering.
  - [ ] Treat `success: false`, missing `data`, an HTTP error, or inconsistent pagination as a Category-load failure rather than silently enabling a partial selector.
  - [ ] Expose distinct loading, loaded-empty, and failed states near the selector and keep submission disabled until non-empty reference data is ready.
- [ ] Coordinate edit initialization with paginated options (AC: 3, 4)
  - [ ] Load the Product detail and complete Category option set without relying on subscription timing; combine the streams or explicitly gate the patch until both have succeeded.
  - [ ] Patch the existing Product form values after options are available so Angular Material can match the numeric `categoryId`, including when the matching Category arrived on page 1 or later.
  - [ ] Verify numeric identity is preserved; do not compare string route/DOM values to numeric Category IDs.
  - [ ] If the Product's Category is absent after a successful complete traversal, show a reference-data error and prevent submission instead of presenting a blank selection or silently choosing another Category.
- [ ] Add focused Angular tests (AC: 2-7)
  - [ ] Add Category service tests for URL/parameters and typed `ApiResponse<PageResponse<Category>>` handling.
  - [ ] Add Product form component tests for first-page options and a two-or-more-page sequence where the existing Product's Category appears only on a later page; assert the select and form control contain that exact numeric ID.
  - [ ] Assert traversal uses server metadata, stops at the last page, and sends a deterministic sort rather than assuming one page.
  - [ ] Cover required validation, create and update payload IDs, SKU disabled on edit, Category loading, zero-category, unsuccessful-envelope, and HTTP-error behavior.
  - [ ] Run the affected Jasmine/Karma tests and the frontend production build; no backend test or application-code change is required for this story.

## Dev Notes

### Inspected current implementation

- `ProductFormComponent` already implements the required reactive create/edit flow. It currently renders `categoryId` as `<input matInput type="number">`, initializes the control as `number | null` with `Validators.required`, disables SKU in edit mode, loads Product detail from the route ID, and sends the raw numeric `categoryId` in the existing request types.
- The form currently has only one `loading` flag, used while saving. This story needs separate Category reference-data state so loading options cannot be confused with an in-flight create/update request.
- No frontend Category feature/model/service exists. Add the smallest typed read-only boundary needed here; Category CRUD UI is outside scope.
- The backend already exposes authorized `GET /api/v1/categories` and `GET /api/v1/categories/{id}` endpoints. The list accepts `page`, `size`, `sortBy`, and `sortDir`, defaults to page 0/size 20, and returns the standard `ApiResponse<PageResponse<CategoryResponse>>` envelope.
- `CategoryResponse` contains `Long id`, `String name`, `String code`, nullable `Long parentId`, and `LocalDateTime createdAt`. The frontend should model JSON numbers as `number`, `parentId` as `number | null`, and the timestamp as `string`.
- `PageResponse` provides `content`, `page`, `size`, `totalElements`, `totalPages`, and `last`. These fields provide the termination condition for complete option loading.
- The Category repository list is genuinely paginated. A single request with the default page size cannot satisfy the edit preselection requirement when the existing Category is on a later page.

### Pagination-safe preselection design

Use the paginated list as the authoritative dropdown source and accumulate all pages before declaring Category data ready. For edit mode, coordinate that completed option stream with `getProductById`; patch the form only after both results are valid. This removes the race in which `categoryId` is patched before its `mat-option` exists and directly covers later-page Categories.

A compliant flow is:

1. Request Category page 0 with a bounded size and deterministic ascending sort.
2. Read `last`/`totalPages`, request each next zero-based page, and accumulate unique Category IDs.
3. In create mode, enable the form once a non-empty complete set is available.
4. In edit mode, combine the complete Category set with Product detail, verify the Product's Category exists, then patch all Product fields.
5. On any page failure or missing selected Category, keep submission disabled and expose the reference-data problem.

Do not solve preselection by increasing the page size to an assumed maximum, patching with a string ID, selecting the first option as a fallback, or firing uncoordinated nested subscriptions. `GET /categories/{id}` may be used as a defensive lookup only if the implementation still provides a complete, navigable option strategy; it must not mask a failed or partial paginated load.

### Contract and compatibility guidance

- Keep `Product.categoryId`, `CreateProductRequest.categoryId`, and `UpdateProductRequest.categoryId` unchanged. The backend intentionally models the foreign key as a plain ID.
- No backend endpoint, DTO, mapper, repository, entity, security rule, or migration change is needed.
- Preserve zero-based pagination and the standard envelopes. Centralize HTTP in the Category feature service; `ProductFormComponent` must not inject `HttpClient`.
- Pass Category sort parameters supported by `CategoryController`; `name` is a mapped entity property and `ASC` is accepted by Spring's `Sort.Direction` parser.
- De-duplication is defensive against page-boundary movement while data changes; a duplicate ID must not create duplicate `mat-option` values.

### UI and accessibility guidance

- Use `mat-label` text `Category` rather than exposing the implementation term `Category ID`.
- Disable the select while options are loading or unavailable. Provide visible loading, empty, and failure text associated with the field; do not rely only on a snackbar.
- Do not auto-select the first Category in create mode. The user must make the required choice.
- Existing Material form-field conventions and `.full-width` styling should be retained.

### Testing guidance

- Use typed `ApiResponse<PageResponse<Category>>` fixtures with page 0 reporting `last: false` and a later page reporting `last: true`. Put the edited Product's Category only in the later response.
- Assert requests are zero-based and stop after the final page. Include an empty final content case consistent with `totalPages: 0` for the no-Categories state.
- In the edit test, wait for both Product and Category observables, run change detection, then assert both the reactive control value and rendered Material selection.
- Assert submitted create/update objects contain a number, not a string, and retain every pre-existing request field.
- Verify partial options are not treated as usable after a later page fails.

### Expected affected files

- `frontend/src/app/features/categories/category.model.ts` (new)
- `frontend/src/app/features/categories/category.service.ts` (new)
- `frontend/src/app/features/categories/category.service.spec.ts` (new)
- `frontend/src/app/features/products/product-form/product-form.component.ts`
- `frontend/src/app/features/products/product-form/product-form.component.spec.ts` (new)

Explicitly do not modify backend code, Product request/detail models, Product routes, Product list behavior, persistence, migrations, or authorization.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.4]
- [Source: `_bmad-output/project-context.md` — REQ-STP-F-102 status and critical frontend rules]
- [Source: `Project 4 - StockPulse.md` — REQ-STP-F-102 and Category endpoint table]
- [Source: current `product-form.component.ts`, `product.model.ts`, and `product.service.ts` — existing form behavior and Product contracts]
- [Source: current `CategoryController.java`, `CategoryService.java`, `CategoryServiceImpl.java`, and `CategoryResponse.java` — paginated list/detail API and response contract]
- [Source: current frontend and backend `PageResponse` models — zero-based pagination metadata]
- [Source: `_bmad-output/project-documentation/frontend-implementation-patterns.md` and `development-and-testing.md`]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

