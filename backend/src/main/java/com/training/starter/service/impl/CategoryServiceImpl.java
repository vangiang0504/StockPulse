package com.training.starter.service.impl;

import com.training.starter.dto.request.CreateCategoryRequest;
import com.training.starter.dto.request.UpdateCategoryRequest;
import com.training.starter.dto.response.CategoryResponse;
import com.training.starter.entity.Category;
import com.training.starter.exception.BadRequestException;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.CategoryMapper;
import com.training.starter.repository.CategoryRepository;
import com.training.starter.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(Pageable pageable) {
        log.debug("Fetching all categories with pagination");
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        log.debug("Fetching category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        log.debug("Creating new category with code: {}", request.code());
        if (categoryRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Category", "code", request.code());
        }
        if (request.parentId() != null && !categoryRepository.existsById(request.parentId())) {
            throw new ResourceNotFoundException("Category", request.parentId());
        }

        Category category = categoryMapper.toEntity(request);
        category = categoryRepository.save(category);
        log.info("Successfully created category with id: {}", category.getId());

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        log.debug("Updating category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        validateParent(id, request.parentId());

        categoryMapper.updateEntity(category, request);
        category = categoryRepository.save(category);
        log.info("Successfully updated category with id: {}", category.getId());

        return categoryMapper.toResponse(category);
    }

    /**
     * A category must not be its own parent, and the parent chain must stay acyclic.
     * Without this check a cycle (A -> B -> A) would make any walk up the tree —
     * breadcrumbs, nested dropdowns — loop forever.
     */
    private void validateParent(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new BadRequestException("Category cannot be its own parent");
        }
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Category", parentId);
        }

        Long ancestorId = parentId;
        while (ancestorId != null) {
            if (ancestorId.equals(categoryId)) {
                throw new BadRequestException("Circular category hierarchy is not allowed");
            }
            ancestorId = categoryRepository.findById(ancestorId)
                    .map(Category::getParentId)
                    .orElse(null);
        }
    }
}
