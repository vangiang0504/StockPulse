package com.training.starter.service;

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
import com.training.starter.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void create_validRequest_returnsProductResponse() {
        // Given
        var request = new CreateProductRequest("SKU-001", "Test Product", "Desc", 1L, "PCS", 10, 100, 20, 50);
        var entity = buildProduct(1L, "SKU-001", "Test Product");
        var response = buildProductResponse(1L, "SKU-001", "Test Product");

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = productService.create(request);

        // Then
        assertThat(result.sku()).isEqualTo("SKU-001");
        assertThat(result.name()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_duplicateSku_throwsDuplicateResourceException() {
        // Given
        var request = new CreateProductRequest("SKU-DUP", "Test", null, 1L, "PCS", 10, 100, 20, 50);
        when(productRepository.existsBySku("SKU-DUP")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsProductResponse() {
        // Given
        var entity = buildProduct(1L, "SKU-001", "Test Product");
        var response = buildProductResponse(1L, "SKU-001", "Test Product");

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(productMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = productService.getById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("SKU-001");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        // Given
        var entity = buildProduct(1L, "SKU-001", "Old Name");
        var request = new UpdateProductRequest("New Name", null, null, null, null, null, null, null, null);
        var response = buildProductResponse(1L, "SKU-001", "New Name");

        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = productService.update(1L, request);

        // Then
        assertThat(result.name()).isEqualTo("New Name");
        verify(productMapper).updateEntity(entity, request);
        verify(productRepository).save(entity);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        // Given
        var request = new UpdateProductRequest("New Name", null, null, null, null, null, null, null, null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void delete_existingProduct_deletesSuccessfully() {
        // Given
        var entity = buildProduct(1L, "SKU-001", "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        productService.delete(1L);

        // Then
        verify(productRepository).delete(entity);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }

    @Test
    void getAll_returnsPageOfProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        var entity = buildProduct(1L, "SKU-001", "Test");
        var summary = new ProductSummaryResponse(1L, "SKU-001", "Test", 1L, "PCS", 10, 20, true, LocalDateTime.now());
        Page<Product> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productMapper.toSummaryResponse(entity)).thenReturn(summary);

        // When
        var result = productService.getAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sku()).isEqualTo("SKU-001");
        assertThat(result.getContent().get(0).minStock()).isEqualTo(10);
        assertThat(result.getContent().get(0).reorderPoint()).isEqualTo(20);
    }

    @Test
    void search_validQuery_returnsPageOfProducts() {
        // Given
        String query = "  SKU-001   Test  ";
        String normalizedQuery = "SKU-001 Test";
        Pageable pageable = PageRequest.of(0, 10);
        var entity = buildProduct(1L, "SKU-001", "Test");
        var summary = new ProductSummaryResponse(1L, "SKU-001", "Test", 1L, "PCS", 10, 20, true, LocalDateTime.now());
        Page<Product> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productRepository.searchByVector(normalizedQuery, pageable)).thenReturn(page);
        when(productMapper.toSummaryResponse(entity)).thenReturn(summary);

        // When
        var result = productService.search(query, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sku()).isEqualTo("SKU-001");
        verify(productRepository).searchByVector(normalizedQuery, pageable);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t\r\n"})
    void search_blankQuery_throwsBadRequestWithoutCallingRepository(String query) {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> productService.search(query, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Search text must not be blank");

        verify(productRepository, never()).searchByVector(any(), any());
    }

    @Test
    void search_defaultControllerSort_translatesMappedPropertyForNativeQuery() {
        Pageable controllerPageable = PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt");
        Pageable nativePageable = PageRequest.of(0, 20, Sort.Direction.DESC, "created_at");
        Page<Product> page = new PageImpl<>(List.of(), nativePageable, 0);
        when(productRepository.searchByVector("phone", nativePageable)).thenReturn(page);

        var result = productService.search("phone", controllerPageable);

        assertThat(result).isEmpty();
        verify(productRepository).searchByVector("phone", nativePageable);
    }

    private Product buildProduct(Long id, String sku, String name) {
        Product product = Product.builder()
                .sku(sku)
                .name(name)
                .categoryId(1L)
                .unit("PCS")
                .minStock(10)
                .maxStock(100)
                .reorderPoint(20)
                .reorderQuantity(50)
                .active(true)
                .build();
        product.setId(id);
        return product;
    }

    private ProductResponse buildProductResponse(Long id, String sku, String name) {
        return new ProductResponse(id, sku, name, null, 1L, "PCS", 10, 100, 20, 50, true, LocalDateTime.now());
    }
}
