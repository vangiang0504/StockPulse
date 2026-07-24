package com.training.starter.repository.projection;

public interface StockThresholdProjection {

    Long getProductId();

    Long getWarehouseId();

    Integer getQuantity();

    Integer getReorderPoint();
}
