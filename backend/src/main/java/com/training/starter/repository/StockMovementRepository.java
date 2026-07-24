package com.training.starter.repository;

import com.training.starter.entity.StockMovement;
import com.training.starter.enums.MovementStatus;
import com.training.starter.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    boolean existsByReferenceNo(String referenceNo);

    Optional<StockMovement> findByReferenceNo(String referenceNo);

    @Query("""
            SELECT movement
            FROM StockMovement movement
            WHERE (:type IS NULL OR movement.type = :type)
              AND (:status IS NULL OR movement.status = :status)
              AND (
                    :warehouseId IS NULL
                    OR movement.warehouseId = :warehouseId
                    OR movement.destWarehouseId = :warehouseId
              )
            """)
    Page<StockMovement> findAllWithFilters(
            @Param("type") MovementType type,
            @Param("status") MovementStatus status,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);
}
