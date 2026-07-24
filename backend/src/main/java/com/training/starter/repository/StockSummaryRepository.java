package com.training.starter.repository;

import com.training.starter.entity.StockLevel;
import com.training.starter.repository.projection.StockSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockSummaryRepository extends Repository<StockLevel, Long> {

    @Query(
            value = """
                    SELECT
                        product_id AS "productId",
                        sku AS "sku",
                        product_name AS "productName",
                        category_name AS "categoryName",
                        warehouse_id AS "warehouseId",
                        warehouse_name AS "warehouseName",
                        quantity AS "quantity",
                        reserved_quantity AS "reservedQuantity",
                        available_quantity AS "availableQuantity",
                        min_stock AS "minStock",
                        reorder_point AS "reorderPoint",
                        stock_status AS "stockStatus"
                    FROM mv_stock_summary
                    WHERE (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                      AND (:productId IS NULL OR product_id = :productId)
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM mv_stock_summary
                    WHERE (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                      AND (:productId IS NULL OR product_id = :productId)
                    """,
            nativeQuery = true)
    Page<StockSummaryProjection> findAllWithFilters(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            Pageable pageable);
}
