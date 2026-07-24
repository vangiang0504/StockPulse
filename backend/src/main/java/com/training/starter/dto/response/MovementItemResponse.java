package com.training.starter.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Integer quantity,
        BigDecimal unitCost,
        String batchNumber,
        LocalDate expiryDate,
        String notes
) {}
