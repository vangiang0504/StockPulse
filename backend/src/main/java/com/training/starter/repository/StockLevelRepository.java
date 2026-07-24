package com.training.starter.repository;

import com.training.starter.entity.StockLevel;
import com.training.starter.repository.projection.StockLevelProjection;
import com.training.starter.repository.projection.StockThresholdProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sl
            FROM StockLevel sl
            WHERE sl.warehouseId IN :warehouseIds
              AND sl.productId IN :productIds
            ORDER BY sl.productId ASC, sl.warehouseId ASC
            """)
    List<StockLevel> findAllForUpdate(
            @Param("warehouseIds") List<Long> warehouseIds,
            @Param("productIds") List<Long> productIds);

    @Query("""
            SELECT
                sl.productId AS productId,
                sl.warehouseId AS warehouseId,
                sl.quantity AS quantity,
                p.reorderPoint AS reorderPoint
            FROM StockLevel sl
            JOIN Product p ON p.id = sl.productId
            WHERE sl.warehouseId IN :warehouseIds
              AND sl.productId IN :productIds
            ORDER BY sl.productId ASC, sl.warehouseId ASC
            """)
    List<StockThresholdProjection> findAffectedWithThresholds(
            @Param("warehouseIds") List<Long> warehouseIds,
            @Param("productIds") List<Long> productIds);

    @Query(
            value = """
                    SELECT
                        sl.id AS id,
                        sl.productId AS productId,
                        p.sku AS productSku,
                        p.name AS productName,
                        sl.warehouseId AS warehouseId,
                        w.code AS warehouseCode,
                        w.name AS warehouseName,
                        sl.quantity AS quantity,
                        sl.reservedQuantity AS reservedQuantity,
                        (sl.quantity - sl.reservedQuantity) AS availableQuantity,
                        p.reorderPoint AS reorderPoint,
                        sl.version AS version,
                        sl.updatedAt AS updatedAt
                    FROM StockLevel sl
                    JOIN Product p ON p.id = sl.productId
                    JOIN Warehouse w ON w.id = sl.warehouseId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND (:productId IS NULL OR sl.productId = :productId)
                    """,
            countQuery = """
                    SELECT COUNT(sl)
                    FROM StockLevel sl
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND (:productId IS NULL OR sl.productId = :productId)
                    """)
    Page<StockLevelProjection> findAllWithDisplayData(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            Pageable pageable);

    @Query(
            value = """
                    SELECT
                        sl.id AS id,
                        sl.productId AS productId,
                        p.sku AS productSku,
                        p.name AS productName,
                        sl.warehouseId AS warehouseId,
                        w.code AS warehouseCode,
                        w.name AS warehouseName,
                        sl.quantity AS quantity,
                        sl.reservedQuantity AS reservedQuantity,
                        (sl.quantity - sl.reservedQuantity) AS availableQuantity,
                        p.reorderPoint AS reorderPoint,
                        sl.version AS version,
                        sl.updatedAt AS updatedAt
                    FROM StockLevel sl
                    JOIN Product p ON p.id = sl.productId
                    JOIN Warehouse w ON w.id = sl.warehouseId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND sl.quantity <= p.reorderPoint
                    """,
            countQuery = """
                    SELECT COUNT(sl)
                    FROM StockLevel sl
                    JOIN Product p ON p.id = sl.productId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND sl.quantity <= p.reorderPoint
                    """)
    Page<StockLevelProjection> findLowStockWithDisplayData(
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);
}
