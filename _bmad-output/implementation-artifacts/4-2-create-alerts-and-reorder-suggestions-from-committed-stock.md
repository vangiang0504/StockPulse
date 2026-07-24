# Story 4.2: Create alerts and reorder suggestions from committed stock

Status: in-progress

## Requirements

- REQ-STP-B-208
- REQ-STP-B-209
- REQ-STP-B-304

## Delivered for REQ-STP-B-208

- [x] Add `StockUpdateConsumer` on `stock.update.queue`.
- [x] Read committed stock and product-specific reorder points in one batch query.
- [x] Evaluate every affected product/warehouse pair without updating stock a second time.
- [x] Publish a versioned `StockLowEvent` when quantity is at or below the reorder point.
- [x] Classify zero quantity as `OUT_OF_STOCK` and other threshold breaches as `LOW_STOCK`.
- [x] Derive a stable low-event ID from source event, warehouse, and product for safe redelivery.
- [x] Reject missing committed stock pairs so infrastructure retry/DLQ can handle inconsistent processing later.
- [x] Verify normal, boundary, zero-stock, transfer, missing-level, redelivery-ID, JSON, and routing behavior.

## Delivered for the reorder portion of REQ-STP-B-209

- [x] Add a `ReorderSuggestion` entity aligned with the existing V5 table.
- [x] Consume `StockLowEvent` messages from `reorder.suggestion.queue`.
- [x] Create pending suggestions using the Product-specific `reorderQuantity`.
- [x] Use PostgreSQL `ON CONFLICT DO NOTHING` with the existing partial unique index so redelivery cannot duplicate a pending product/warehouse suggestion.
- [x] Verify creation, duplicate delivery, and missing-product behavior.

## Remaining

- [ ] REQ-STP-B-304: persist idempotent active `StockAlert` records.
- [ ] Expose the alert and reorder lifecycle endpoints included in Story 4.2.

## Verification

- `StockUpdateConsumerTest`, `StockEventPublisherTest`, `RabbitMQConfigTest`, and `StockMovementServiceTest`: 40 tests passed.
- REQ-STP-B-208 and the reorder portion of REQ-STP-B-209 are complete; Story 4.2 remains in progress because REQ-STP-B-304 and the lifecycle endpoints remain.
