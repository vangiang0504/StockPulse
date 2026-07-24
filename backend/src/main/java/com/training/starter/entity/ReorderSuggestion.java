package com.training.starter.entity;

import com.training.starter.enums.ReorderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reorder_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderSuggestion extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "suggested_quantity", nullable = false)
    private Integer suggestedQuantity;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReorderStatus status = ReorderStatus.PENDING;
}
