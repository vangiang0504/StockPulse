package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        Long parentId,
        LocalDateTime createdAt
) {}
