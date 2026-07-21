package com.training.starter.mapper;

import com.training.starter.entity.Product;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @Test
    void toSummaryResponse_includesStockThresholds() {
        Product product = Product.builder()
                .sku("SKU-001")
                .name("Test Product")
                .categoryId(1L)
                .unit("PCS")
                .minStock(12)
                .reorderPoint(24)
                .active(true)
                .build();
        product.setId(1L);

        var response = productMapper.toSummaryResponse(product);

        assertThat(response.minStock()).isEqualTo(12);
        assertThat(response.reorderPoint()).isEqualTo(24);
    }
}
