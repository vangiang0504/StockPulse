package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record ProductSummaryResponse(
        Long id,
        String sku,
        String name,
        Long categoryId,
        String unit,
        Integer minStock,
        Integer reorderPoint,
        Boolean active,
        LocalDateTime createdAt
) {}
