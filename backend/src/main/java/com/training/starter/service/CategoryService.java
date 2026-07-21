package com.training.starter.service;

import com.training.starter.dto.request.CreateCategoryRequest;
import com.training.starter.dto.request.UpdateCategoryRequest;
import com.training.starter.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponse> getAll(Pageable pageable);

    CategoryResponse getById(Long id);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(Long id, UpdateCategoryRequest request);
}
