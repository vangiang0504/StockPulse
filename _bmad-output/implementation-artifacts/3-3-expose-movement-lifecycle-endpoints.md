# Story 3.3: Expose movement lifecycle endpoints

Status: done

## Requirement

- REQ-STP-B-205

## Delivered

- [x] Create import, export, and transfer endpoints.
- [x] Paginated movement list with type, status, and warehouse filters.
- [x] Movement detail endpoint.
- [x] Approve and complete endpoints.
- [x] STAFF/MANAGER/ADMIN read/create/complete authorization.
- [x] MANAGER/ADMIN approve authorization.
- [x] Standard API envelopes, validation, sorting, and OpenAPI schemas.

## Verification

- `MovementControllerTest`: 12 tests passing.
- Non-Docker backend regression suite: 110 tests passing at completion.

