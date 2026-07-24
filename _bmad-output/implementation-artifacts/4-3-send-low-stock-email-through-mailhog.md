# Story 4.3: Send low-stock email through MailHog

Status: done

## Requirements

- Email portion of REQ-STP-B-209

## Delivered

- [x] Add `EmailConsumer` on `email.alert.queue`.
- [x] Resolve Product and Warehouse display data for each low-stock event.
- [x] Include severity, product, warehouse, current stock, reorder point, suggested quantity, movement ID, and event ID in the message.
- [x] Use existing SMTP/MailHog configuration with environment-backed `ALERT_EMAIL_FROM` and `ALERT_EMAIL_RECIPIENTS`.
- [x] Suppress successful redelivery duplicates with a 30-day Redis event marker.
- [x] Mark delivery only after SMTP succeeds; failed sends throw for future B-305 retry/DLQ handling.
- [x] Verify message content, configured recipients, duplicate suppression, SMTP failure, missing configuration, and Redis marker behavior.

## Verification

- `EmailConsumerTest`, `RedisEmailDeliveryTrackerTest`, `ReorderConsumerTest`, `StockUpdateConsumerTest`, and `RabbitMQConfigTest`: 19 tests passed.
- Live MailHog delivery remains part of the later Docker/Testcontainers integration pass.
