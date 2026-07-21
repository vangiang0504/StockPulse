# Frontend Implementation Patterns

## Application Structure

The Angular application is organized by responsibility:

- `core/` — singleton services, guards, interceptor, and shared API models.
- `features/` — domain-specific components, services, and models.
- `layout/` — authenticated and authentication page shells.
- `shared/` — reusable components such as confirmation, pagination, and loading UI.

All components are standalone. New components should declare their Angular, router, form, and Material dependencies in the component `imports` array rather than adding NgModules.

## Routing

- Routes lazy-load components with `loadComponent`.
- Authenticated routes are children of `MainLayoutComponent` and guarded by `authGuard`.
- Login/register routes use `AuthLayoutComponent`.
- Feature paths use plural resource names; create/edit paths are explicit.
- The wildcard route redirects to the application root.

Add StockPulse routes under the authenticated main layout. Role-sensitive screens/actions should be hidden appropriately, but the API must enforce the actual role rule.

## HTTP and API Models

Feature services:

- Are injectable singletons with `providedIn: 'root'`.
- Build their base URL from `environment.apiUrl` (`/api/v1`).
- Use `HttpClient` only inside the service, not directly in components.
- Return typed `Observable<ApiResponse<T>>` values.
- Use `PageResponse<T>` for server-paginated results.

The global functional JWT interceptor attaches the access token from local storage. Feature services must not repeat authorization-header logic.

The Product feature places interfaces in `product.model.ts`; the User feature currently defines them in `user.service.ts`. For new StockPulse domains, prefer the Product separation: keep models and request interfaces in a feature model file and HTTP behavior in the service.

## List Components

The User and Product lists establish these patterns:

- Angular Material table with named column definitions.
- `MatPaginator` configured with 10, 20, and 50 row choices.
- Zero-based `currentPage` and a server-reported `totalElements`.
- A page event updates page/size and reloads data.
- `RouterLink` for create/edit navigation.
- `NotificationService` for failures and mutation success.
- A shared confirmation dialog before destructive operations.

The Product list improves on the User list by displaying explicit loading and empty states. New StockPulse list screens should follow the Product version and always provide loading, empty, error, and content states.

## Form Components

- Use reactive forms built with `FormBuilder`.
- Mirror backend required fields, lengths, numeric minimums, and default values.
- Use route parameters to switch between create and edit mode.
- Disable immutable fields during edit (username and SKU in current features).
- Clear create-only validators when entering edit mode (User password).
- Patch data returned from the detail endpoint.
- Prevent submission while invalid or already loading.
- Send a typed create or update request through the feature service.
- Show a snackbar and navigate back to the list only after success.
- On failure, clear loading and prefer the server error message with a stable fallback.

The existing code uses non-null assertions after form validation. New code should keep strict typing and minimize assertions by using non-nullable form controls where practical.

## Authentication

`AuthService` stores these values in local storage:

- `access_token`
- `refresh_token`
- `username`
- `role`

The guard checks only for access-token presence. The interceptor removes tokens and redirects to login on HTTP 401. Logout clears authentication values and navigates to login.

Current limitations to preserve awareness of:

- There is no automatic refresh-on-401 flow even though a refresh endpoint exists.
- Token presence is treated as authenticated client-side; validity is decided by the backend.
- Role strings are stored but no reusable role guard/directive exists.

StockPulse may extend these behaviors, but changes should remain centralized in core authentication/authorization utilities.

## UI Conventions

- Angular Material is the component system.
- The prebuilt indigo/pink theme is configured globally.
- Global helpers include `.container`, `.full-width`, `.spacer`, and success/error snackbar classes.
- `NotificationService` uses top-right snackbars: three seconds for success/info and five seconds for errors.
- Inline templates and small inline layout styles are established in current components.

Stock-status displays must include text in addition to color. Follow the project brief's red out-of-stock, orange low-stock, and yellow overstock semantics.

## TypeScript Rules

The compiler and Angular template compiler are strict. New code must satisfy:

- `strict`
- `noImplicitOverride`
- `noPropertyAccessFromIndexSignature`
- `noImplicitReturns`
- `noFallthroughCasesInSwitch`
- strict dependency injection, inputs, and templates

Avoid `any`; model nullable and optional values explicitly.

