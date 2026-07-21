package com.training.starter.mapper;

import com.training.starter.dto.request.CreateCategoryRequest;
import com.training.starter.dto.request.UpdateCategoryRequest;
import com.training.starter.dto.response.CategoryResponse;
import com.training.starter.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    Category toEntity(CreateCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void updateEntity(@MappingTarget Category category, UpdateCategoryRequest request);
}
