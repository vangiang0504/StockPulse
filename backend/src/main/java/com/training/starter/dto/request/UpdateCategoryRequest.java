package com.training.starter.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        Long parentId
) {}
