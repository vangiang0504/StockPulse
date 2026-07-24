package com.training.starter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Line item of a {@link StockMovement}. The backing {@code stock_movement_items} table has no
 * {@code created_at}/{@code updated_at} columns, so this entity intentionally does NOT extend
 * {@link BaseEntity} — doing so would fail Hibernate schema validation at startup. The link to the
 * parent movement is a plain {@code Long movementId}, keeping with the project rule of scalar
 * foreign keys instead of JPA relationships.
 */
@Entity
@Table(name = "stock_movement_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_id", nullable = false)
    private Long movementId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
