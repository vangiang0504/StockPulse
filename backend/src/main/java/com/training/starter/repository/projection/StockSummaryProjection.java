package com.training.starter.repository.projection;

public interface StockSummaryProjection {

    Long getProductId();

    String getSku();

    String getProductName();

    String getCategoryName();

    Long getWarehouseId();

    String getWarehouseName();

    Integer getQuantity();

    Integer getReservedQuantity();

    Integer getAvailableQuantity();

    Integer getMinStock();

    Integer getReorderPoint();

    String getStockStatus();
}
