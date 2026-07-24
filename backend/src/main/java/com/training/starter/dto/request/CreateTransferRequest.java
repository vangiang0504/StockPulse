package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateTransferRequest(
        @Schema(description = "Source warehouse the stock leaves", example = "1")
        @NotNull(message = "Source warehouse ID is required")
        @Positive(message = "Source warehouse ID must be positive")
        Long warehouseId,

        @Schema(
                description = "Destination warehouse the stock arrives at; must differ from source",
                example = "2")
        @NotNull(message = "Destination warehouse ID is required")
        @Positive(message = "Destination warehouse ID must be positive")
        Long destWarehouseId,

        @Schema(description = "Optional movement notes")
        String notes,

        @Schema(description = "Line items; at least one is required")
        @NotEmpty(message = "At least one item is required")
        @Valid
        List<CreateMovementItemRequest> items
) {}
