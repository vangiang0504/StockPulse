# Story 4.1: Configure StockPulse events, retry, and dead-letter routing

Status: in-progress

## Requirements

- REQ-STP-B-207
- REQ-STP-B-305

## Delivered for REQ-STP-B-207

- [x] Preserve the generic `training.*` topology and add durable topic exchange `stock.exchange`.
- [x] Add durable `stock.update.queue`, `reorder.suggestion.queue`, `email.alert.queue`, and `audit.queue`.
- [x] Bind the queues with `stock.*.completed`, `stock.alert.low`, `stock.alert.#`, and `stock.#`.
- [x] Add versioned `StockMovementCompletedEvent` with stable event ID, movement identity/type, affected product and warehouse IDs, and timestamp.
- [x] Publish movement-completed events only after the stock transaction commits; publish nothing after rollback.
- [x] Include event ID, correlation ID, event type, and schema version in AMQP message metadata.
- [x] Verify exact topology, JSON round-trip, routing keys, commit behavior, rollback behavior, and movement-service publication.

## Remaining for REQ-STP-B-305

- [ ] Configure bounded consumer retry.
- [ ] Add dead-letter exchange, dead-letter queues, and failure metadata for every StockPulse queue.
- [ ] Add retry-to-DLQ integration verification.

## Verification

- `RabbitMQConfigTest`, `StockEventPublisherTest`, and `StockMovementServiceTest`: 32 tests passed.
- REQ-STP-B-207 is complete; Story 4.1 remains in progress because REQ-STP-B-305 is intentionally outside this implementation.
