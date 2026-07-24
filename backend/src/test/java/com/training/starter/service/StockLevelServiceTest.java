package com.training.starter.service;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.dto.response.StockSummaryResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
import com.training.starter.entity.Warehouse;
import com.training.starter.enums.StockStatus;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.StockLevelMapper;
import com.training.starter.mapper.StockSummaryMapper;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.StockLevelRepository;
import com.training.starter.repository.StockSummaryRepository;
import com.training.starter.repository.WarehouseRepository;
import com.training.starter.repository.projection.StockLevelProjection;
import com.training.starter.repository.projection.StockSummaryProjection;
import com.training.starter.service.impl.StockLevelServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockLevelServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockSummaryRepository stockSummaryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelMapper stockLevelMapper;

    @Mock
    private StockSummaryMapper stockSummaryMapper;

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private StockLevelProjection stockLevelProjection;

    @Mock
    private StockSummaryProjection stockSummaryProjection;

    @InjectMocks
    private StockLevelServiceImpl stockLevelService;

    @Test
    void getByWarehouseAndProduct_found_returnsEnrichedStockLevelResponse() {
        // Given
        StockLevel stockLevel = StockLevel.builder()
                .id(7L)
                .productId(11L)
                .warehouseId(22L)
                .quantity(40)
                .reservedQuantity(7)
                .version(3L)
                .updatedAt(LocalDateTime.of(2026, 7, 23, 10, 0))
                .build();
        Product product = Product.builder()
                .sku("SKU-011")
                .name("Mapped product")
                .reorderPoint(20)
                .build();
        product.setId(11L);
        Warehouse warehouse = Warehouse.builder()
                .code("WH-022")
                .name("Mapped warehouse")
                .build();
        warehouse.setId(22L);
        StockLevelResponse response = new StockLevelResponse(
                7L,
                11L,
                "SKU-011",
                "Mapped product",
                22L,
                "WH-022",
                "Mapped warehouse",
                40,
                7,
                33,
                20,
                3L,
                LocalDateTime.of(2026, 7, 23, 10, 0));

        when(stockLevelRepository.findByWarehouseIdAndProductId(22L, 11L))
                .thenReturn(Optional.of(stockLevel));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(22L)).thenReturn(Optional.of(warehouse));
        when(stockLevelMapper.toResponse(stockLevel, product, warehouse)).thenReturn(response);

        // When
        StockLevelResponse result = stockLevelService.getByWarehouseAndProduct(22L, 11L);

        // Then
        assertThat(result.productSku()).isEqualTo("SKU-011");
        assertThat(result.warehouseCode()).isEqualTo("WH-022");
        assertThat(result.availableQuantity()).isEqualTo(33);
        verify(stockLevelMapper).toResponse(stockLevel, product, warehouse);
        verify(stockCacheService).put(22L, 11L, response);
    }

    @Test
    void getByWarehouseAndProduct_cacheHit_avoidsDatabaseAndMapper() {
        // Given
        StockLevelResponse cachedResponse = buildResponse();
        when(stockCacheService.get(22L, 11L))
                .thenReturn(Optional.of(cachedResponse));

        // When
        StockLevelResponse result =
                stockLevelService.getByWarehouseAndProduct(22L, 11L);

        // Then
        assertThat(result).isSameAs(cachedResponse);
        verifyNoInteractions(
                stockLevelRepository,
                productRepository,
                warehouseRepository,
                stockLevelMapper);
        verify(stockCacheService, never()).put(22L, 11L, cachedResponse);
    }

    @Test
    void getByWarehouseAndProduct_secondQueryUsesCacheAfterFirstQuery() {
        // Given
        StockLevel stockLevel = StockLevel.builder()
                .productId(11L)
                .warehouseId(22L)
                .quantity(40)
                .reservedQuantity(7)
                .build();
        Product product = Product.builder()
                .sku("SKU-011")
                .name("Product")
                .build();
        Warehouse warehouse = Warehouse.builder()
                .code("WH-022")
                .name("Warehouse")
                .build();
        StockLevelResponse response = buildResponse();

        when(stockCacheService.get(22L, 11L))
                .thenReturn(Optional.empty(), Optional.of(response));
        when(stockLevelRepository.findByWarehouseIdAndProductId(22L, 11L))
                .thenReturn(Optional.of(stockLevel));
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(22L)).thenReturn(Optional.of(warehouse));
        when(stockLevelMapper.toResponse(stockLevel, product, warehouse))
                .thenReturn(response);

        // When
        StockLevelResponse first =
                stockLevelService.getByWarehouseAndProduct(22L, 11L);
        StockLevelResponse second =
                stockLevelService.getByWarehouseAndProduct(22L, 11L);

        // Then
        assertThat(first).isSameAs(response);
        assertThat(second).isSameAs(response);
        verify(stockLevelRepository, times(1))
                .findByWarehouseIdAndProductId(22L, 11L);
        verify(stockLevelMapper, times(1))
                .toResponse(stockLevel, product, warehouse);
        verify(stockCacheService, times(1)).put(22L, 11L, response);
    }

    @Test
    void getByWarehouseAndProduct_notFound_throwsResourceNotFoundException() {
        // Given
        when(stockLevelRepository.findByWarehouseIdAndProductId(22L, 999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockLevelService.getByWarehouseAndProduct(22L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("warehouse id: 22")
                .hasMessageContaining("product id: 999");
        verifyNoInteractions(productRepository, warehouseRepository, stockLevelMapper);
        verify(stockCacheService, never()).put(
                anyLong(),
                anyLong(),
                any(StockLevelResponse.class));
    }

    @Test
    void getAll_filtersProvided_returnsMappedPageAndPreservesMetadata() {
        // Given
        var pageable = PageRequest.of(1, 2);
        var projectionPage = new PageImpl<>(
                List.of(stockLevelProjection), pageable, 3);
        StockLevelResponse response = buildResponse();

        when(stockLevelRepository.findAllWithDisplayData(22L, 11L, pageable))
                .thenReturn(projectionPage);
        when(stockLevelMapper.toResponse(stockLevelProjection)).thenReturn(response);

        // When
        var result = stockLevelService.getAll(22L, 11L, pageable);

        // Then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(stockLevelRepository).findAllWithDisplayData(22L, 11L, pageable);
    }

    @Test
    void getAll_filtersOmitted_delegatesNullFilters() {
        // Given
        var pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<StockLevelProjection>(List.of(), pageable, 0);

        when(stockLevelRepository.findAllWithDisplayData(null, null, pageable))
                .thenReturn(projectionPage);

        // When
        var result = stockLevelService.getAll(null, null, pageable);

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(stockLevelRepository).findAllWithDisplayData(null, null, pageable);
        verifyNoInteractions(stockLevelMapper);
    }

    @Test
    void getLowStock_warehouseFilterProvided_returnsMappedPage() {
        // Given
        var pageable = PageRequest.of(0, 10);
        var projectionPage = new PageImpl<>(
                List.of(stockLevelProjection), pageable, 1);
        StockLevelResponse response = buildResponse();

        when(stockLevelRepository.findLowStockWithDisplayData(22L, pageable))
                .thenReturn(projectionPage);
        when(stockLevelMapper.toResponse(stockLevelProjection)).thenReturn(response);

        // When
        var result = stockLevelService.getLowStock(22L, pageable);

        // Then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(stockLevelRepository).findLowStockWithDisplayData(22L, pageable);
    }

    @Test
    void getLowStock_warehouseFilterOmitted_delegatesNullFilter() {
        // Given
        var pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<StockLevelProjection>(List.of(), pageable, 0);

        when(stockLevelRepository.findLowStockWithDisplayData(null, pageable))
                .thenReturn(projectionPage);

        // When
        var result = stockLevelService.getLowStock(null, pageable);

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(stockLevelRepository).findLowStockWithDisplayData(null, pageable);
        verifyNoInteractions(stockLevelMapper);
    }

    @Test
    void getSummary_filtersProvided_returnsMappedPageAndPreservesMetadata() {
        // Given
        var pageable = PageRequest.of(1, 2);
        var projectionPage = new PageImpl<>(
                List.of(stockSummaryProjection), pageable, 3);
        StockSummaryResponse response = buildSummaryResponse();

        when(stockSummaryRepository.findAllWithFilters(22L, 11L, pageable))
                .thenReturn(projectionPage);
        when(stockSummaryMapper.toResponse(stockSummaryProjection)).thenReturn(response);

        // When
        var result = stockLevelService.getSummary(22L, 11L, pageable);

        // Then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(stockSummaryRepository).findAllWithFilters(22L, 11L, pageable);
    }

    @Test
    void getSummary_filtersOmitted_delegatesNullFilters() {
        // Given
        var pageable = PageRequest.of(0, 20);
        var projectionPage = new PageImpl<StockSummaryProjection>(List.of(), pageable, 0);

        when(stockSummaryRepository.findAllWithFilters(null, null, pageable))
                .thenReturn(projectionPage);

        // When
        var result = stockLevelService.getSummary(null, null, pageable);

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(stockSummaryRepository).findAllWithFilters(null, null, pageable);
        verifyNoInteractions(stockSummaryMapper);
    }

    private StockLevelResponse buildResponse() {
        return new StockLevelResponse(
                7L,
                11L,
                "SKU-011",
                "Mapped product",
                22L,
                "WH-022",
                "Mapped warehouse",
                40,
                7,
                33,
                20,
                3L,
                LocalDateTime.of(2026, 7, 23, 10, 0));
    }

    private StockSummaryResponse buildSummaryResponse() {
        return new StockSummaryResponse(
                11L,
                "SKU-011",
                "Mapped product",
                "Mapped category",
                22L,
                "Mapped warehouse",
                40,
                7,
                33,
                10,
                20,
                StockStatus.NORMAL);
    }
}
