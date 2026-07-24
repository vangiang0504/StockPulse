package com.training.starter.repository;

import com.training.starter.entity.StockMovementItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementItemRepository extends JpaRepository<StockMovementItem, Long> {

    List<StockMovementItem> findByMovementId(Long movementId);
}
