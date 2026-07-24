package com.training.starter.dto.response;

import com.training.starter.enums.StockStatus;

public record StockSummaryResponse(
        Long productId,
        String sku,
        String productName,
        String categoryName,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer minStock,
        Integer reorderPoint,
        StockStatus stockStatus) {
}
