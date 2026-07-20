package com.training.starter.repository;

import com.training.starter.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    Page<Product> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(String sku, String name, Pageable pageable);

    @Query(
        value = "SELECT * FROM products p WHERE p.search_vector @@ plainto_tsquery(:query)",
        countQuery = "SELECT count(*) FROM products p WHERE p.search_vector @@ plainto_tsquery(:query)",
        nativeQuery = true
    )
    Page<Product> searchByVector(@Param("query") String query, Pageable pageable);
}
