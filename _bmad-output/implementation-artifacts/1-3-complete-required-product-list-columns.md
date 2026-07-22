# Story 1.3: Complete Required Product List Columns

Status: ready-for-dev

## Story

As a StockPulse staff member,
I want Min Stock and Reorder Point displayed in the existing Product list,
so that I can assess replenishment thresholds without opening each Product.

## Acceptance Criteria

1. `ProductSummaryResponse` includes non-null `minStock` and `reorderPoint` values mapped from the existing `Product` entity for every Product list and search result.
2. The frontend `ProductSummary` model mirrors the extended backend summary contract with numeric `minStock` and `reorderPoint` properties.
3. The existing Product Material table displays `Min Stock` and `Reorder Point` columns, with each row showing the corresponding server-provided values.
4. The Product list retains its existing SKU, Name, Category, Unit, Status, and Actions columns, loading spinner, empty state, API-error notification, edit link, and 10/20/50 server-pagination behavior with zero-based page indices.
5. `GET /api/v1/products` and `GET /api/v1/products/search` retain their existing paths, authorization, request parameters, sorting, and `ApiResponse<PageResponse<ProductSummaryResponse>>` envelope; Product detail/create/update contracts remain unchanged.
6. Focused backend coverage verifies both new summary fields are mapped and returned, and Angular component coverage verifies the two headers/values, existing states, and paginator-driven reload behavior.

## Tasks / Subtasks

- [ ] Extend the existing backend Product summary contract (AC: 1, 5)
  - [ ] Add `Integer minStock` and `Integer reorderPoint` to `ProductSummaryResponse` using names that MapStruct can map directly from `Product`.
  - [ ] Keep the response a Java record and preserve the existing summary fields and endpoint envelope.
  - [ ] Do not add repository queries, entity fields, migrations, or a replacement list endpoint; both values already exist on `Product` and in V2.
- [ ] Verify backend summary mapping and list behavior (AC: 1, 5, 6)
  - [ ] Update every test construction of `ProductSummaryResponse` for the extended record signature.
  - [ ] Add a focused mapper assertion using the generated `ProductMapper` implementation, or equivalent service/controller coverage that does not mock away mapping, proving `minStock` and `reorderPoint` come from the entity.
  - [ ] Verify the existing `getAll` and search page mapping still return the extended summary without changing pagination metadata.
- [ ] Extend the typed frontend summary model (AC: 2, 5)
  - [ ] Add required numeric `minStock` and `reorderPoint` fields to `ProductSummary`.
  - [ ] Keep `ProductService.getProducts` and its typed `ApiResponse<PageResponse<ProductSummary>>` contract unchanged.
- [ ] Add the two columns to the existing Product list (AC: 3, 4)
  - [ ] Add `minStock` and `reorderPoint` Material column definitions with the exact headers `Min Stock` and `Reorder Point`.
  - [ ] Add both keys to `displayedColumns` in a readable order before Status/Actions.
  - [ ] Render numeric zero as `0`; do not use truthiness-based fallbacks that hide valid zero thresholds.
  - [ ] Preserve the existing component, routes, service calls, loading/empty/error handling, status chip, edit action, and paginator.
- [ ] Add focused Angular component coverage (AC: 3, 4, 6)
  - [ ] Create `product-list.component.spec.ts` using representative `ProductSummary` rows with distinct Min Stock and Reorder Point values.
  - [ ] Assert both new headers and row values render while the pre-existing columns remain present.
  - [ ] Cover loading and empty rendering, API failure notification, and a page event updating the zero-based page index/page size before reloading.
  - [ ] Run the frontend production build and affected Jasmine/Karma tests; run affected backend tests after the record signature change.

## Dev Notes

### Inspected current implementation

- `Product` already defines `minStock` and `reorderPoint` as `Integer` fields, with defaults and V2-backed columns. No persistence change is needed.
- `ProductResponse` and the frontend detail `Product` interface already include both values, so create/edit behavior is not part of this story.
- `ProductSummaryResponse` currently contains only id, SKU, name, category ID, unit, active, and created time. `ProductMapper.toSummaryResponse(Product)` uses MapStruct convention mapping, so adding identically named record components is sufficient; avoid hand-written mapping unless compilation proves otherwise.
- `ProductServiceImpl.getAll` and `search` already map `Page<Product>` through `ProductMapper.toSummaryResponse`. Both flows will receive the new fields through the shared summary DTO without service or repository changes.
- `ProductListComponent` is an Angular 17 standalone component with an inline Material-table template. Its current `displayedColumns` are SKU, Name, Category, Unit, Status, and Actions. It already uses server pagination with page sizes 10/20/50 and zero-based indices.
- On an API error, the component clears loading and calls `NotificationService.error('Failed to load products')`. Preserve this established error behavior rather than introducing a broader state-management redesign.
- No Product frontend spec currently exists, so this story adds focused component coverage rather than modifying an established Product list test.

### Contract and compatibility guidance

- Treat both fields as required numbers in `ProductSummary`: the backend entity and schema make them non-null, and the list should display the server value directly.
- Preserve the existing JSON property naming (`minStock`, `reorderPoint`). Do not introduce aliases or nested threshold objects.
- Adding record components changes the Java constructor signature. Update current direct constructors in `ProductServiceTest` and any new/changed tests so the backend test sources compile.
- The change is additive at the JSON level. Do not modify `ProductController`, `ProductService`, `ProductServiceImpl`, `ProductRepository`, `ProductResponse`, request DTOs, or pagination wrappers unless a failing test reveals a direct compile-only consequence.
- The search endpoint uses the same summary mapper. Keep its full-text search behavior and tests intact while updating summary fixtures for the new record signature.

### UI and accessibility guidance

- Use the existing table markup and Angular interpolation; threshold values are plain numeric business data and need no chip, color, or client-side calculation.
- Keep descriptive header text and normal table-cell semantics. Do not abbreviate the headings in a way that makes their meaning unclear.
- Preserve the current responsive/layout conventions. If the additional columns create horizontal pressure, use a narrowly scoped table/container overflow treatment rather than removing existing required columns.
- Keep Category rendering as its current category ID. Replacing it with category display data belongs to the separate Product category workflow and is outside this story.

### Testing guidance

- For backend mapping, prefer a small `ProductMapperTest` using `Mappers.getMapper(ProductMapper.class)` so the new fields are verified rather than stubbed. If the project test setup makes generated-mapper construction impractical, use focused Spring mapper coverage instead.
- Update the existing `ProductServiceTest` summary fixtures with explicit, unequal values such as 10 and 20, and assert both values on the returned item where useful.
- Angular tests should use typed response fixtures matching `ApiResponse<PageResponse<ProductSummary>>`; avoid `any` under strict TypeScript.
- Component tests should query rendered header/cell text and invoke or emit a `PageEvent` to verify `getProducts(pageIndex, pageSize)` is called with zero-based values.
- Use `fakeAsync`/`tick` only if needed by Material rendering; synchronous `of(...)` and `throwError(...)` streams should otherwise keep the tests focused.

### Expected affected files

- `backend/src/main/java/com/training/starter/dto/response/ProductSummaryResponse.java`
- `backend/src/test/java/com/training/starter/service/ProductServiceTest.java`
- `backend/src/test/java/com/training/starter/mapper/ProductMapperTest.java` (new, or equivalent focused mapper test)
- `frontend/src/app/features/products/product.model.ts`
- `frontend/src/app/features/products/product-list/product-list.component.ts`
- `frontend/src/app/features/products/product-list/product-list.component.spec.ts` (new)

Explicitly do not modify Product persistence, Flyway migrations, Product request/detail DTOs, repositories, routes, forms, authorization, or API envelope/pagination types.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.3]
- [Source: `_bmad-output/project-context.md` — REQ-STP-F-101 status and critical frontend rules]
- [Source: current `Product.java` and `V2__create_warehouse_tables.sql` — existing non-null threshold fields and columns]
- [Source: current `ProductSummaryResponse.java` and `ProductMapper.java` — summary gap and convention-based mapping]
- [Source: current `ProductServiceImpl.java` — shared list/search summary mapping]
- [Source: current `product.model.ts`, `product.service.ts`, and `product-list.component.ts` — typed list contract, columns, states, and pagination]
- [Source: current `ProductServiceTest.java` — direct summary-record construction and backend test conventions]
- [Source: `_bmad-output/project-documentation/frontend-implementation-patterns.md` and `development-and-testing.md`]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

