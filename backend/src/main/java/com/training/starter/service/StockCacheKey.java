package com.training.starter.service;

public record StockCacheKey(Long warehouseId, Long productId) {

    public String redisKey() {
        return "stock:%d:%d".formatted(warehouseId, productId);
    }
}
