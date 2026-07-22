package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseRequest(
        @Schema(description = "New warehouse display name", example = "Central Distribution Center")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Schema(description = "New warehouse address", example = "200 Main Street")
        String address,

        @Schema(description = "Whether the warehouse is active", example = "true")
        Boolean active
) {}
