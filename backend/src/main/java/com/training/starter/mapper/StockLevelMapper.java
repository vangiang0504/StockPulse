package com.training.starter.mapper;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
import com.training.starter.entity.Warehouse;
import com.training.starter.repository.projection.StockLevelProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockLevelMapper {

    @Mapping(target = "id", source = "stockLevel.id")
    @Mapping(target = "productId", source = "stockLevel.productId")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "warehouseId", source = "stockLevel.warehouseId")
    @Mapping(target = "warehouseCode", source = "warehouse.code")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "quantity", source = "stockLevel.quantity")
    @Mapping(target = "reservedQuantity", source = "stockLevel.reservedQuantity")
    @Mapping(
            target = "availableQuantity",
            expression = "java(stockLevel.getQuantity() - stockLevel.getReservedQuantity())")
    @Mapping(target = "reorderPoint", source = "product.reorderPoint")
    @Mapping(target = "version", source = "stockLevel.version")
    @Mapping(target = "updatedAt", source = "stockLevel.updatedAt")
    StockLevelResponse toResponse(
            StockLevel stockLevel,
            Product product,
            Warehouse warehouse);

    StockLevelResponse toResponse(StockLevelProjection projection);
}
