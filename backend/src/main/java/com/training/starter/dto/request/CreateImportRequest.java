package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateImportRequest(
        @Schema(description = "Destination warehouse receiving the imported stock", example = "1")
        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        @Schema(description = "Optional movement notes")
        String notes,

        @Schema(description = "Line items; at least one is required")
        @NotEmpty(message = "At least one item is required")
        @Valid
        List<CreateMovementItemRequest> items
) {}
