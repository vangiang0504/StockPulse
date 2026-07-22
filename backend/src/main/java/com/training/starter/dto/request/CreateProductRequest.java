package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @Schema(description = "Unique product SKU; immutable after creation", example = "PRD-001")
        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,

        @Schema(description = "Product display name", example = "Wireless Mouse")
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Schema(description = "Optional product description", example = "Ergonomic wireless mouse")
        String description,

        @Schema(description = "Existing category ID", example = "1")
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "Stock-keeping unit of measure", example = "piece")
        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Schema(description = "Minimum desired stock", example = "10")
        @NotNull(message = "Min stock is required")
        @Min(value = 0, message = "Min stock cannot be negative")
        Integer minStock,

        @Schema(description = "Maximum desired stock", example = "100")
        @NotNull(message = "Max stock is required")
        @Min(value = 0, message = "Max stock cannot be negative")
        Integer maxStock,

        @Schema(description = "Quantity at which reordering is recommended", example = "20")
        @NotNull(message = "Reorder point is required")
        @Min(value = 0, message = "Reorder point cannot be negative")
        Integer reorderPoint,

        @Schema(description = "Quantity recommended for a reorder", example = "50")
        @NotNull(message = "Reorder quantity is required")
        @Min(value = 1, message = "Reorder quantity must be at least 1")
        Integer reorderQuantity
) {}
