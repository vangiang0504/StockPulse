# Story 3.2: Approve and complete movements with ordered locking

Status: done

## Requirements

- REQ-STP-B-203
- REQ-STP-T-201
- Completion portion of REQ-STP-B-202

## Delivered

- [x] Approve only `PENDING_APPROVAL` movements and record `approvedBy`.
- [x] Complete only `APPROVED` movements.
- [x] Lock affected stock levels with `PESSIMISTIC_WRITE` in product/warehouse order.
- [x] Apply IMPORT, EXPORT, and TRANSFER changes atomically with batched persistence.
- [x] Reject exports/transfers that would make stock negative.
- [x] Cover import, export, transfer, insufficient stock, invalid state, and lock ordering.

## Verification

- `StockMovementServiceTest`: 26 tests passing after Stories 3.2 and 3.3.
- Non-Docker backend regression suite passed after implementation.

