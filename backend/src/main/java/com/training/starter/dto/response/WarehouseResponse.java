package com.training.starter.dto.response;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String name,
        String code,
        String address,
        Boolean active,
        LocalDateTime createdAt
) {}
