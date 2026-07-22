# Story 1.5: Add the Warehouse List Route and Navigation

Status: ready-for-dev

## Story

As an authorized StockPulse staff member,
I want to open a paginated Warehouse list from the application sidebar,
so that I can view Warehouse locations and their operational status without calling the API directly.

## Acceptance Criteria

1. A standalone `WarehouseListComponent` under `features/warehouses/warehouse-list` loads Warehouses through a typed frontend service backed by the existing zero-based `GET /api/v1/warehouses` endpoint and its `ApiResponse<PageResponse<WarehouseResponse>>` envelope; no backend Warehouse behavior is recreated or changed.
2. The Material table displays the exact business columns `Name`, `Code`, `Address`, and `Status`. Status is derived from the response's `active` boolean and is rendered as readable `Active` or `Inactive` text, with styling that does not make color the only status indicator.
3. The list has distinct loading, loaded-empty, loaded-content, and API-error states. An unsuccessful or missing-data envelope is treated as an error rather than as a successful empty page, and stale rows/pagination totals are not retained after a failed load.
4. Server pagination preserves zero-based page indices, starts with the existing API default size of 20, exposes page-size options 10/20/50, uses `totalElements` for paginator length, and reloads with the selected `pageIndex` and `pageSize`.
5. A lazy `warehouses` child route is added under the existing `MainLayoutComponent` route tree, which is protected by `authGuard`. Navigating to `/warehouses` while unauthenticated follows the existing redirect-to-login behavior.
6. The main sidebar includes a `Warehouses` navigation item linking to `/warehouses`, with the established Material icon/list and `routerLinkActive` conventions. It is shown for authenticated `STAFF`, `MANAGER`, and `ADMIN` roles that can use the Warehouse read API, and is not exposed to the starter-only `USER` role.
7. Focused Angular tests cover the typed service request/response contract, list loading/content/empty/error states, status labels, paginator-driven calls, the lazy route registration/protection, and sidebar visibility/navigation for StockPulse roles.
8. This story is read-only UI scope: do not add Warehouse create, edit, detail, deactivate, or action controls; do not add corresponding routes, forms, service methods, backend changes, persistence changes, or migrations.

## Tasks / Subtasks

- [ ] Add the typed Warehouse frontend boundary (AC: 1, 4, 8)
  - [ ] Create `warehouse.model.ts` with a `Warehouse` interface matching `WarehouseResponse`: numeric `id`, string `name`, `code`, `address`, boolean `active`, and string `createdAt`.
  - [ ] Create a root-provided `WarehouseService` under `features/warehouses` with a read-only `getWarehouses(page = 0, size = 20)` method.
  - [ ] Build the URL from `environment.apiUrl` and call `GET /warehouses` with zero-based `page` and `size`, returning `Observable<ApiResponse<PageResponse<Warehouse>>>` without `any`, manual authorization headers, or envelope duplication.
  - [ ] Do not add create, update, detail, or deactivation methods for future scope.
- [ ] Build the standalone Warehouse list (AC: 1-4, 8)
  - [ ] Add `WarehouseListComponent` under `features/warehouses/warehouse-list` using the existing standalone Angular and inline Material table conventions.
  - [ ] Render Name, Code, Address, and Status only; map `active === true` to visible `Active` text and `active === false` to visible `Inactive` text.
  - [ ] Import only the Angular Material modules needed for the table, paginator, status treatment, and loading/empty/error presentation.
  - [ ] Keep explicit component state for rows, `loading`, `error`, `totalElements`, `currentPage`, and `pageSize`; reset error before a retry and settle loading on every success/failure path.
  - [ ] Treat HTTP failures, `success: false`, and absent `data` as errors; clear Warehouse rows and paginator totals and show a visible retryable error state while also using the established notification service where consistent.
  - [ ] Show a distinct empty message only after a successful response with no content. Do not render empty or error messaging while the initial request is loading.
  - [ ] Configure `MatPaginator` with length from `totalElements`, initial size 20, options 10/20/50, and reload from every emitted `PageEvent` using its zero-based `pageIndex`.
  - [ ] Do not render New, Edit, action, or detail controls.
- [ ] Register the protected route and sidebar navigation (AC: 5, 6, 8)
  - [ ] Add a lazy `/warehouses` child route under the existing `MainLayoutComponent` branch in `app.routes.ts`, loading `WarehouseListComponent` with `loadComponent`.
  - [ ] Retain `authGuard` on the parent layout as the authentication boundary; do not duplicate or replace the backend's Warehouse role authorization.
  - [ ] Add a Warehouses sidebar item using `routerLink="/warehouses"`, `routerLinkActive="active"`, and an appropriate established Material icon.
  - [ ] Gate only this sidebar item with `AuthService.getRole()` membership in `STAFF`, `MANAGER`, or `ADMIN`; preserve all existing sidebar items and logout behavior.
  - [ ] Do not add Warehouse create/edit/detail routes.
- [ ] Add focused Angular coverage (AC: 1-8)
  - [ ] Add `warehouse.service.spec.ts` with `HttpTestingController` assertions for `/warehouses?page=0&size=20`, a non-default zero-based page/size, and typed envelope propagation.
  - [ ] Add `warehouse-list.component.spec.ts` covering the loading indicator, all four headers and representative values, both status labels, successful empty state, unsuccessful/missing-data envelopes, HTTP error behavior, cleared stale data, and loading settlement.
  - [ ] Assert a paginator event updates page index and size and invokes `getWarehouses` with the emitted zero-based values; verify paginator length comes from `totalElements`.
  - [ ] Add or extend route coverage to prove `warehouses` is a lazy child of the guarded main layout and that unauthenticated navigation is redirected by the existing guard.
  - [ ] Add or extend `MainLayoutComponent` coverage for the Warehouse link's path, active-link setup, visibility for `STAFF`/`MANAGER`/`ADMIN`, and absence for `USER`.
  - [ ] Run the focused Jasmine/Karma tests and frontend production build. No backend tests are required because the existing Warehouse API and REQ-STP-T-102 coverage are unchanged.

## Dev Notes

### Inspected current implementation

- `WarehouseController` already exposes authorized `GET /api/v1/warehouses` for `STAFF`, `MANAGER`, and `ADMIN`. It accepts `page`, `size`, `sortBy`, and `sortDir`, defaults to page 0/size 20, and returns `ApiResponse<PageResponse<WarehouseResponse>>`. Story 1.5 consumes this contract and makes no backend change.
- `WarehouseResponse` contains `Long id`, `String name`, `String code`, `String address`, `Boolean active`, and `LocalDateTime createdAt`. In TypeScript these map to `number`, `string`, `string`, `string`, `boolean`, and `string` respectively.
- There is currently no Warehouse frontend feature directory. Keep the new boundary small and read-only instead of mirroring backend CRUD that this story does not expose.
- `ProductListComponent` supplies the closest list pattern: a standalone inline Material table, spinner, empty state, notification on load failure, a paginator with 10/20/50 options, and zero-based service calls. Reuse its conventions while correcting the distinction between unsuccessful envelopes, empty results, and HTTP errors.
- `ProductService` supplies the closest typed service pattern: root-provided service, URL based on `environment.apiUrl`, and shared `ApiResponse`/`PageResponse` types.
- `app.routes.ts` lazy-loads feature components beneath a `MainLayoutComponent` route protected by `authGuard`. A Warehouse child automatically remains behind that authentication boundary.
- `MainLayoutComponent` owns the inline Material sidebar. `AuthService.getRole()` returns the role string stored at login and can be used for the narrow StockPulse-role visibility check.

### Contract and compatibility guidance

- Use `/warehouses` relative to `environment.apiUrl`; the configured API base already includes `/api/v1`. Do not hard-code the full server origin or duplicate `/api/v1`.
- Preserve the shared envelope and pagination interfaces. The service returns the typed envelope; the component owns UI-state interpretation.
- Preserve zero-based page numbers end to end. `MatPaginator.pageIndex` can be passed directly to the backend and must not be incremented for display-oriented numbering.
- Do not request client-side pagination or use the current rows' length as the total. `PageResponse.totalElements` is the paginator authority.
- Do not convert `active` into a separate API model property. Render the business label in the component and retain the boolean contract.
- The sidebar condition is presentation-level discoverability, not security. Backend `@PreAuthorize` remains authoritative. The starter `USER` role must not gain Warehouse read access through this UI work.
- Keep existing Product and User routes/navigation unchanged. REQ-STP-F-104 is partially implemented; this story supplies only its missing Warehouse route and sidebar portion.

### UI and accessibility guidance

- Use an `<h2>` page title such as `Warehouses` and normal Material table header/cell semantics.
- Provide visible status text. A chip or class may distinguish states visually, but `Active`/`Inactive` must remain readable without color perception.
- Long addresses should remain readable without removing required columns; use narrowly scoped wrapping or horizontal overflow if needed.
- Give loading, empty, and error messages clear text. If a retry button is supplied, it reloads the current page and size; it does not introduce another route or form.
- Avoid action-column placeholders. Read-only scope should be visually clear rather than suggesting unavailable create/edit functionality.

### Testing guidance

- Use strictly typed `ApiResponse<PageResponse<Warehouse>>` fixtures with distinct `page`, `size`, `totalElements`, `totalPages`, and `last` values.
- Service tests should verify the request method and query parameters with `HttpTestingController`; JWT attachment belongs to the existing interceptor and should not be duplicated in service assertions.
- Component tests should query rendered header/cell text and validate both boolean status outcomes. Include a previously loaded row before an error assertion to prove stale content is cleared.
- For route coverage, inspect or navigate through the exported `routes` using Angular Router testing support. Avoid invoking the lazy import by duplicating component wiring in production code.
- For sidebar role tests, stub `AuthService.getRole()` separately for `STAFF`, `MANAGER`, `ADMIN`, and `USER`. Also provide the authenticated token state required by the route test without weakening `authGuard`.

### Expected affected files

- `frontend/src/app/features/warehouses/warehouse.model.ts` (new)
- `frontend/src/app/features/warehouses/warehouse.service.ts` (new)
- `frontend/src/app/features/warehouses/warehouse.service.spec.ts` (new)
- `frontend/src/app/features/warehouses/warehouse-list/warehouse-list.component.ts` (new)
- `frontend/src/app/features/warehouses/warehouse-list/warehouse-list.component.spec.ts` (new)
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/app.routes.spec.ts` (new or existing route spec)
- `frontend/src/app/layout/main-layout/main-layout.component.ts`
- `frontend/src/app/layout/main-layout/main-layout.component.spec.ts` (new or existing layout spec)

Explicitly do not modify backend code, Warehouse DTOs/entities/services/controllers, authorization rules, Flyway migrations, Product routes, or any Warehouse create/edit/detail functionality.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.5]
- [Source: `_bmad-output/project-context.md` — REQ-STP-F-103 and REQ-STP-F-104 implementation status]
- [Source: `Project 4 - StockPulse.md` — REQ-STP-F-103 and REQ-STP-F-104]
- [Source: current `WarehouseController.java` and `WarehouseResponse.java` — existing paginated read endpoint, role matrix, and response contract]
- [Source: current `product.service.ts` and `product-list.component.ts` — typed frontend service, Material list, state, and pagination patterns]
- [Source: current `app.routes.ts`, `auth.guard.ts`, `main-layout.component.ts`, and `auth.service.ts` — protected lazy routing, sidebar, and role storage patterns]
- [Source: current shared `api-response.model.ts` and `page-response.model.ts` — frontend envelope and zero-based page metadata]
- [Source: `_bmad-output/project-documentation/frontend-implementation-patterns.md` and `development-and-testing.md`]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

