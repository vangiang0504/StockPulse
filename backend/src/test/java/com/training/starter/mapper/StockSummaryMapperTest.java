package com.training.starter.mapper;

import com.training.starter.enums.StockStatus;
import com.training.starter.repository.projection.StockSummaryProjection;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class StockSummaryMapperTest {

    private final StockSummaryMapper stockSummaryMapper =
            Mappers.getMapper(StockSummaryMapper.class);

    @Test
    void toResponse_projection_mapsEveryMaterializedViewField() {
        // Given
        StockSummaryProjection projection = new StockSummaryProjection() {
            @Override
            public Long getProductId() {
                return 11L;
            }

            @Override
            public String getSku() {
                return "SKU-011";
            }

            @Override
            public String getProductName() {
                return "Mapped product";
            }

            @Override
            public String getCategoryName() {
                return null;
            }

            @Override
            public Long getWarehouseId() {
                return 22L;
            }

            @Override
            public String getWarehouseName() {
                return "Mapped warehouse";
            }

            @Override
            public Integer getQuantity() {
                return 40;
            }

            @Override
            public Integer getReservedQuantity() {
                return 7;
            }

            @Override
            public Integer getAvailableQuantity() {
                return 33;
            }

            @Override
            public Integer getMinStock() {
                return 10;
            }

            @Override
            public Integer getReorderPoint() {
                return 20;
            }

            @Override
            public String getStockStatus() {
                return "NORMAL";
            }
        };

        // When
        var response = stockSummaryMapper.toResponse(projection);

        // Then
        assertThat(response.productId()).isEqualTo(11L);
        assertThat(response.sku()).isEqualTo("SKU-011");
        assertThat(response.productName()).isEqualTo("Mapped product");
        assertThat(response.categoryName()).isNull();
        assertThat(response.warehouseId()).isEqualTo(22L);
        assertThat(response.warehouseName()).isEqualTo("Mapped warehouse");
        assertThat(response.quantity()).isEqualTo(40);
        assertThat(response.reservedQuantity()).isEqualTo(7);
        assertThat(response.availableQuantity()).isEqualTo(33);
        assertThat(response.minStock()).isEqualTo(10);
        assertThat(response.reorderPoint()).isEqualTo(20);
        assertThat(response.stockStatus()).isEqualTo(StockStatus.NORMAL);
    }
}
