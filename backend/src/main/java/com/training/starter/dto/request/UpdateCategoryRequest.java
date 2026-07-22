package com.training.starter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Schema(description = "New category display name", example = "Computer Accessories")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Schema(description = "New existing parent category ID", example = "1")
        Long parentId
) {}
