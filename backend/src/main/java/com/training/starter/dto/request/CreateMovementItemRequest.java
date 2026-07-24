package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMovementItemRequest(
        @Schema(description = "Product ID for this line item", example = "1")
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @Schema(description = "Positive quantity to move", example = "10")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity,

        @Schema(description = "Optional unit cost; must not be negative", example = "12.50")
        @DecimalMin(value = "0.0", message = "Unit cost must not be negative")
        BigDecimal unitCost,

        @Schema(description = "Optional batch number", example = "B-2026-07")
        @Size(max = 50, message = "Batch number must not exceed 50 characters")
        String batchNumber,

        @Schema(description = "Optional expiry date", example = "2027-01-31")
        LocalDate expiryDate,

        @Schema(description = "Optional line-item notes")
        String notes
) {}
