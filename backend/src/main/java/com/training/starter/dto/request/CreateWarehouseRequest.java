package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
    @Schema(description = "Warehouse display name", example = "Central Warehouse")
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Schema(description = "Unique warehouse code; immutable after creation", example = "WH-CENTRAL")
    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    String code,

    @Schema(description = "Optional warehouse address", example = "100 Main Street")
    String address
) {}
