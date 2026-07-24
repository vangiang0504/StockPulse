package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record StockLevelResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer reorderPoint,
        Long version,
        LocalDateTime updatedAt
) {}
