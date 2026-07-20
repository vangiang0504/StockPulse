package com.training.starter.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 50, message = "SKU must not exceed 50 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Unit is required")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @NotNull(message = "Min stock is required")
        @Min(value = 0, message = "Min stock cannot be negative")
        Integer minStock,

        @NotNull(message = "Max stock is required")
        @Min(value = 0, message = "Max stock cannot be negative")
        Integer maxStock,

        @NotNull(message = "Reorder point is required")
        @Min(value = 0, message = "Reorder point cannot be negative")
        Integer reorderPoint,

        @NotNull(message = "Reorder quantity is required")
        @Min(value = 1, message = "Reorder quantity must be at least 1")
        Integer reorderQuantity
) {}
