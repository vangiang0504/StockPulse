package com.training.starter.repository.projection;

import java.time.LocalDateTime;

public interface StockLevelProjection {

    Long getId();

    Long getProductId();

    String getProductSku();

    String getProductName();

    Long getWarehouseId();

    String getWarehouseCode();

    String getWarehouseName();

    Integer getQuantity();

    Integer getReservedQuantity();

    Integer getAvailableQuantity();

    Integer getReorderPoint();

    Long getVersion();

    LocalDateTime getUpdatedAt();
}
