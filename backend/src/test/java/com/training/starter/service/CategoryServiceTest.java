package com.training.starter.service;

import com.training.starter.dto.request.CreateCategoryRequest;
import com.training.starter.dto.request.UpdateCategoryRequest;
import com.training.starter.dto.response.CategoryResponse;
import com.training.starter.entity.Category;
import com.training.starter.exception.BadRequestException;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.CategoryMapper;
import com.training.starter.repository.CategoryRepository;
import com.training.starter.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void create_validRequest_returnsCategoryResponse() {
        // Given
        var request = new CreateCategoryRequest("Laptops", "CAT-LAP", 2L);
        var entity = buildCategory(3L, "Laptops", "CAT-LAP", 2L);
        var response = buildCategoryResponse(3L, "Laptops", "CAT-LAP", 2L);

        when(categoryRepository.existsByCode("CAT-LAP")).thenReturn(false);
        when(categoryRepository.existsById(2L)).thenReturn(true);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(any(Category.class))).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = categoryService.create(request);

        // Then
        assertThat(result.code()).isEqualTo("CAT-LAP");
        assertThat(result.parentId()).isEqualTo(2L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_rootCategory_skipsParentLookup() {
        // Given - parentId is null, so no parent existence check should happen
        var request = new CreateCategoryRequest("Electronics", "CAT-ELEC", null);
        var entity = buildCategory(2L, "Electronics", "CAT-ELEC", null);
        var response = buildCategoryResponse(2L, "Electronics", "CAT-ELEC", null);

        when(categoryRepository.existsByCode("CAT-ELEC")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(any(Category.class))).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = categoryService.create(request);

        // Then
        assertThat(result.parentId()).isNull();
        verify(categoryRepository, never()).existsById(any());
    }

    @Test
    void create_duplicateCode_throwsDuplicateResourceException() {
        // Given
        var request = new CreateCategoryRequest("Duplicate", "CAT-DUP", null);
        when(categoryRepository.existsByCode("CAT-DUP")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_parentNotFound_throwsResourceNotFoundException() {
        // Given
        var request = new CreateCategoryRequest("Orphan", "CAT-ORP", 999L);
        when(categoryRepository.existsByCode("CAT-ORP")).thenReturn(false);
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsCategoryResponse() {
        // Given
        var entity = buildCategory(1L, "Smart Phone", "CAT-SP", null);
        var response = buildCategoryResponse(1L, "Smart Phone", "CAT-SP", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = categoryService.getById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("CAT-SP");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        // Given
        var entity = buildCategory(1L, "Old Name", "CAT-SP", null);
        var request = new UpdateCategoryRequest("New Name", null);
        var response = buildCategoryResponse(1L, "New Name", "CAT-SP", null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = categoryService.update(1L, request);

        // Then
        assertThat(result.name()).isEqualTo("New Name");
        verify(categoryMapper).updateEntity(entity, request);
        verify(categoryRepository).save(entity);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        // Given
        var request = new UpdateCategoryRequest("New Name", null);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_selfAsParent_throwsBadRequestException() {
        // Given - category 1 tries to become its own parent
        var entity = buildCategory(1L, "Smart Phone", "CAT-SP", null);
        var request = new UpdateCategoryRequest(null, 1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When & Then
        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own parent");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_circularHierarchy_throwsBadRequestException() {
        // Given - category 5 is already a child of 3.
        // Making 3 a child of 5 would close the loop 3 -> 5 -> 3.
        var entity = buildCategory(3L, "Laptops", "CAT-LAP", null);
        var descendant = buildCategory(5L, "Gaming Laptops", "CAT-GAM", 3L);
        var request = new UpdateCategoryRequest(null, 5L);

        when(categoryRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsById(5L)).thenReturn(true);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(descendant));

        // When & Then
        assertThatThrownBy(() -> categoryService.update(3L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Circular");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_parentNotFound_throwsResourceNotFoundException() {
        // Given
        var entity = buildCategory(1L, "Smart Phone", "CAT-SP", null);
        var request = new UpdateCategoryRequest(null, 999L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getAll_returnsPageOfCategories() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        var entity = buildCategory(1L, "Smart Phone", "CAT-SP", null);
        var response = buildCategoryResponse(1L, "Smart Phone", "CAT-SP", null);
        Page<Category> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(categoryRepository.findAll(pageable)).thenReturn(page);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = categoryService.getAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("CAT-SP");
    }

    private Category buildCategory(Long id, String name, String code, Long parentId) {
        Category category = Category.builder()
                .name(name)
                .code(code)
                .parentId(parentId)
                .build();
        category.setId(id);
        return category;
    }

    private CategoryResponse buildCategoryResponse(Long id, String name, String code, Long parentId) {
        return new CategoryResponse(id, name, code, parentId, LocalDateTime.now());
    }
}
