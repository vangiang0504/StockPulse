package com.training.starter.controller;

import com.training.starter.common.ApiResponse;
import com.training.starter.common.PageResponse;
import com.training.starter.config.OpenApiSchemas;
import com.training.starter.dto.request.CreateCategoryRequest;
import com.training.starter.dto.request.UpdateCategoryRequest;
import com.training.starter.dto.response.CategoryResponse;
import com.training.starter.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "listCategories", summary = "List categories",
            description = "Returns a zero-based page of categories. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.CategoryPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination or sort input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<CategoryResponse>> getAll(
            @Parameter(description = "Zero-based page index", schema = @Schema(defaultValue = "0", minimum = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of categories per page", schema = @Schema(defaultValue = "20", minimum = "1")) @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Category property used for sorting", schema = @Schema(defaultValue = "createdAt")) @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", schema = @Schema(defaultValue = "DESC", allowableValues = {"ASC", "DESC"})) @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        var result = categoryService.getAll(PageRequest.of(page, size, sort));
        return ApiResponse.success(PageResponse.from(result, r -> r));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "getCategory", summary = "Get a category",
            description = "Returns one category by ID. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.CategoryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<CategoryResponse> getById(@Parameter(description = "Category ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createCategory", summary = "Create a category",
            description = "Creates a category. Code is required and becomes immutable. Allowed roles: MANAGER or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.CategoryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category code already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("Category created", categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(operationId = "updateCategory", summary = "Update a category",
            description = "Updates mutable category fields; code is not an update property. Allowed roles: MANAGER or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.CategoryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation, self-parenting, or circular hierarchy", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category or requested parent not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<CategoryResponse> update(
            @Parameter(description = "Category ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success("Category updated", categoryService.update(id, request));
    }
}
