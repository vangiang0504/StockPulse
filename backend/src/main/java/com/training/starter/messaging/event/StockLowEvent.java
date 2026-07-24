package com.training.starter.messaging.event;

import com.training.starter.enums.StockStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockLowEvent(
        String schemaVersion,
        String eventId,
        String sourceEventId,
        Long movementId,
        Long productId,
        Long warehouseId,
        Integer currentQuantity,
        Integer reorderPoint,
        StockStatus stockStatus,
        Instant occurredAt
) implements StockEvent {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    public StockLowEvent {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        eventId = requireText(eventId, "eventId");
        sourceEventId = requireText(sourceEventId, "sourceEventId");
        movementId = Objects.requireNonNull(movementId, "movementId is required");
        productId = Objects.requireNonNull(productId, "productId is required");
        warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
        currentQuantity = requireNonNegative(currentQuantity, "currentQuantity");
        reorderPoint = requireNonNegative(reorderPoint, "reorderPoint");
        stockStatus = Objects.requireNonNull(stockStatus, "stockStatus is required");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");

        if (currentQuantity > reorderPoint) {
            throw new IllegalArgumentException(
                    "currentQuantity must be at or below reorderPoint");
        }
        StockStatus expectedStatus = currentQuantity == 0
                ? StockStatus.OUT_OF_STOCK
                : StockStatus.LOW_STOCK;
        if (stockStatus != expectedStatus) {
            throw new IllegalArgumentException(
                    "stockStatus does not match currentQuantity");
        }
    }

    public static StockLowEvent create(
            StockMovementCompletedEvent source,
            Long productId,
            Long warehouseId,
            Integer currentQuantity,
            Integer reorderPoint) {
        String identity = "%s:%d:%d"
                .formatted(source.eventId(), warehouseId, productId);
        String stableEventId = UUID.nameUUIDFromBytes(
                        identity.getBytes(StandardCharsets.UTF_8))
                .toString();
        StockStatus status = currentQuantity == 0
                ? StockStatus.OUT_OF_STOCK
                : StockStatus.LOW_STOCK;

        return new StockLowEvent(
                CURRENT_SCHEMA_VERSION,
                stableEventId,
                source.eventId(),
                source.movementId(),
                productId,
                warehouseId,
                currentQuantity,
                reorderPoint,
                status,
                Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static Integer requireNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }
}
