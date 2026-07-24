package com.training.starter.repository;

import com.training.starter.entity.ReorderSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReorderSuggestionRepository
        extends JpaRepository<ReorderSuggestion, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO reorder_suggestions (
                        product_id,
                        warehouse_id,
                        suggested_quantity,
                        current_stock,
                        reorder_point,
                        status,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :productId,
                        :warehouseId,
                        :suggestedQuantity,
                        :currentStock,
                        :reorderPoint,
                        'PENDING',
                        NOW(),
                        NOW()
                    )
                    ON CONFLICT (product_id, warehouse_id)
                        WHERE status = 'PENDING'
                    DO NOTHING
                    """,
            nativeQuery = true)
    int insertPendingIfAbsent(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("suggestedQuantity") Integer suggestedQuantity,
            @Param("currentStock") Integer currentStock,
            @Param("reorderPoint") Integer reorderPoint);
}
