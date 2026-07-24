package com.training.starter.mapper;

import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
import com.training.starter.entity.Warehouse;
import com.training.starter.repository.projection.StockLevelProjection;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockLevelMapperTest {

    private final StockLevelMapper stockLevelMapper = Mappers.getMapper(StockLevelMapper.class);

    @Test
    void toResponse_validSources_returnsEnrichedResponseWithAvailableQuantity() {
        // Given
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 23, 10, 0);
        StockLevel stockLevel = StockLevel.builder()
                .id(7L)
                .productId(11L)
                .warehouseId(22L)
                .quantity(40)
                .reservedQuantity(7)
                .version(3L)
                .updatedAt(updatedAt)
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

        // When
        var result = stockLevelMapper.toResponse(stockLevel, product, warehouse);

        // Then
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.productId()).isEqualTo(11L);
        assertThat(result.productSku()).isEqualTo("SKU-011");
        assertThat(result.productName()).isEqualTo("Mapped product");
        assertThat(result.warehouseId()).isEqualTo(22L);
        assertThat(result.warehouseCode()).isEqualTo("WH-022");
        assertThat(result.warehouseName()).isEqualTo("Mapped warehouse");
        assertThat(result.quantity()).isEqualTo(40);
        assertThat(result.reservedQuantity()).isEqualTo(7);
        assertThat(result.availableQuantity()).isEqualTo(33);
        assertThat(result.reorderPoint()).isEqualTo(20);
        assertThat(result.version()).isEqualTo(3L);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponse_projection_returnsEveryProjectedField() {
        // Given
        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 23, 11, 0);
        StockLevelProjection projection = mock(StockLevelProjection.class);
        when(projection.getId()).thenReturn(8L);
        when(projection.getProductId()).thenReturn(12L);
        when(projection.getProductSku()).thenReturn("SKU-012");
        when(projection.getProductName()).thenReturn("Projected product");
        when(projection.getWarehouseId()).thenReturn(23L);
        when(projection.getWarehouseCode()).thenReturn("WH-023");
        when(projection.getWarehouseName()).thenReturn("Projected warehouse");
        when(projection.getQuantity()).thenReturn(50);
        when(projection.getReservedQuantity()).thenReturn(8);
        when(projection.getAvailableQuantity()).thenReturn(42);
        when(projection.getReorderPoint()).thenReturn(30);
        when(projection.getVersion()).thenReturn(4L);
        when(projection.getUpdatedAt()).thenReturn(updatedAt);

        // When
        var result = stockLevelMapper.toResponse(projection);

        // Then
        assertThat(result.id()).isEqualTo(8L);
        assertThat(result.productId()).isEqualTo(12L);
        assertThat(result.productSku()).isEqualTo("SKU-012");
        assertThat(result.productName()).isEqualTo("Projected product");
        assertThat(result.warehouseId()).isEqualTo(23L);
        assertThat(result.warehouseCode()).isEqualTo("WH-023");
        assertThat(result.warehouseName()).isEqualTo("Projected warehouse");
        assertThat(result.quantity()).isEqualTo(50);
        assertThat(result.reservedQuantity()).isEqualTo(8);
        assertThat(result.availableQuantity()).isEqualTo(42);
        assertThat(result.reorderPoint()).isEqualTo(30);
        assertThat(result.version()).isEqualTo(4L);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }
}
