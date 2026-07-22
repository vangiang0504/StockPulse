package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @Schema(description = "New product name", example = "Wireless Mouse Pro")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Schema(description = "New product description")
        String description,

        @Schema(description = "New existing category ID", example = "2")
        Long categoryId,

        @Schema(description = "New unit of measure", example = "piece")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Schema(description = "New minimum desired stock", example = "10")
        @Min(value = 0, message = "Min stock cannot be negative")
        Integer minStock,

        @Schema(description = "New maximum desired stock", example = "100")
        @Min(value = 0, message = "Max stock cannot be negative")
        Integer maxStock,

        @Schema(description = "New reorder point", example = "20")
        @Min(value = 0, message = "Reorder point cannot be negative")
        Integer reorderPoint,

        @Schema(description = "New reorder quantity", example = "50")
        @Min(value = 1, message = "Reorder quantity must be at least 1")
        Integer reorderQuantity,

        @Schema(description = "Whether the product is active", example = "true")
        Boolean active
) {}
