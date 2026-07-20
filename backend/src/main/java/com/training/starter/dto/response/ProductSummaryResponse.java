package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record ProductSummaryResponse(
        Long id,
        String sku,
        String name,
        Long categoryId,
        String unit,
        Boolean active,
        LocalDateTime createdAt
) {}
