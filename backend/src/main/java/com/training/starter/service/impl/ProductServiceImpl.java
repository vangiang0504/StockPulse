package com.training.starter.service.impl;

import com.training.starter.dto.request.CreateProductRequest;
import com.training.starter.dto.request.UpdateProductRequest;
import com.training.starter.dto.response.ProductResponse;
import com.training.starter.dto.response.ProductSummaryResponse;
import com.training.starter.entity.Product;
import com.training.starter.exception.BadRequestException;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.ProductMapper;
import com.training.starter.repository.ProductRepository;
import com.training.starter.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Map<String, String> SEARCH_SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("sku", "sku"),
            Map.entry("name", "name"),
            Map.entry("description", "description"),
            Map.entry("categoryId", "category_id"),
            Map.entry("unit", "unit"),
            Map.entry("minStock", "min_stock"),
            Map.entry("maxStock", "max_stock"),
            Map.entry("reorderPoint", "reorder_point"),
            Map.entry("reorderQuantity", "reorder_quantity"),
            Map.entry("active", "active"),
            Map.entry("createdAt", "created_at"),
            Map.entry("updatedAt", "updated_at")
    );

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getAll(Pageable pageable) {
        log.debug("Fetching all products with pagination");
        return productRepository.findAll(pageable).map(productMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        log.debug("Creating new product with SKU: {}", request.sku());
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product", "sku", request.sku());
        }

        Product product = productMapper.toEntity(request);
        product = productRepository.save(product);
        log.info("Successfully created product with id: {}", product.getId());
        
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        log.debug("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        
        productMapper.updateEntity(product, request);
        product = productRepository.save(product);
        log.info("Successfully updated product with id: {}", product.getId());
        
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Deleting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        productRepository.delete(product);
        log.info("Successfully deleted product with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("Search text must not be blank");
        }

        String normalizedQuery = query.trim().replaceAll("\\s+", " ");
        log.debug("Searching products by query: {}", normalizedQuery);
        return productRepository.searchByVector(normalizedQuery, toNativeSearchPageable(pageable))
                .map(productMapper::toSummaryResponse);
    }

    private Pageable toNativeSearchPageable(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getSort().isUnsorted()) {
            return pageable;
        }

        Sort nativeSort = Sort.by(pageable.getSort().stream()
                .map(order -> {
                    String column = SEARCH_SORT_COLUMNS.get(order.getProperty());
                    if (column == null) {
                        throw new BadRequestException("Unsupported product sort property: " + order.getProperty());
                    }
                    return order.withProperty(column);
                })
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), nativeSort);
    }
}
