package com.training.starter.repository;

import com.training.starter.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    boolean existsByReferenceNo(String referenceNo);

    Optional<StockMovement> findByReferenceNo(String referenceNo);
}
