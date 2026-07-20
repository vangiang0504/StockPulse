package com.training.starter.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        Long categoryId,

        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Min(value = 0, message = "Min stock cannot be negative")
        Integer minStock,

        @Min(value = 0, message = "Max stock cannot be negative")
        Integer maxStock,

        @Min(value = 0, message = "Reorder point cannot be negative")
        Integer reorderPoint,

        @Min(value = 1, message = "Reorder quantity must be at least 1")
        Integer reorderQuantity,

        Boolean active
) {}
