package com.training.starter.service.impl;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.dto.response.StockSummaryResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
import com.training.starter.entity.Warehouse;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.StockLevelMapper;
import com.training.starter.mapper.StockSummaryMapper;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.StockLevelRepository;
import com.training.starter.repository.StockSummaryRepository;
import com.training.starter.repository.WarehouseRepository;
import com.training.starter.service.StockCacheService;
import com.training.starter.service.StockLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockLevelServiceImpl implements StockLevelService {

    private final StockLevelRepository stockLevelRepository;
    private final StockSummaryRepository stockSummaryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLevelMapper stockLevelMapper;
    private final StockSummaryMapper stockSummaryMapper;
    private final StockCacheService stockCacheService;

    @Override
    @Transactional(readOnly = true)
    public StockLevelResponse getByWarehouseAndProduct(Long warehouseId, Long productId) {
        log.debug(
                "Fetching stock level by warehouse id: {} and product id: {}",
                warehouseId,
                productId);

        var cached = stockCacheService.get(warehouseId, productId);
        if (cached.isPresent()) {
            log.debug(
                    "Stock cache hit for warehouse id: {} and product id: {}",
                    warehouseId,
                    productId);
            return cached.get();
        }

        StockLevel stockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock level not found for warehouse id: "
                                + warehouseId
                                + " and product id: "
                                + productId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", warehouseId));

        StockLevelResponse response =
                stockLevelMapper.toResponse(stockLevel, product, warehouse);
        stockCacheService.put(warehouseId, productId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> getAll(
            Long warehouseId,
            Long productId,
            Pageable pageable) {
        log.debug(
                "Fetching stock levels with warehouse id: {} and product id: {}",
                warehouseId,
                productId);
        return stockLevelRepository
                .findAllWithDisplayData(warehouseId, productId, pageable)
                .map(stockLevelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> getLowStock(Long warehouseId, Pageable pageable) {
        log.debug("Fetching low-stock levels with warehouse id: {}", warehouseId);
        return stockLevelRepository
                .findLowStockWithDisplayData(warehouseId, pageable)
                .map(stockLevelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockSummaryResponse> getSummary(
            Long warehouseId,
            Long productId,
            Pageable pageable) {
        log.debug(
                "Fetching stock summary with warehouse id: {} and product id: {}",
                warehouseId,
                productId);
        return stockSummaryRepository
                .findAllWithFilters(warehouseId, productId, pageable)
                .map(stockSummaryMapper::toResponse);
    }
}
