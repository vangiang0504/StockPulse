package com.training.starter.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MovementResponse(
        Long id,
        String referenceNo,
        String type,
        String status,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long destWarehouseId,
        String destWarehouseCode,
        String destWarehouseName,
        String notes,
        Long createdBy,
        Long approvedBy,
        List<MovementItemResponse> items,
        LocalDateTime createdAt
) {}
