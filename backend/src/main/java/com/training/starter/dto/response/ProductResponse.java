package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String unit,
        Integer minStock,
        Integer maxStock,
        Integer reorderPoint,
        Integer reorderQuantity,
        Boolean active,
        LocalDateTime createdAt
) {}
