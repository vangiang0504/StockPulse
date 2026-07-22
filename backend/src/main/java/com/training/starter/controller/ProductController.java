package com.training.starter.controller;

import com.training.starter.common.ApiResponse;
import com.training.starter.common.PageResponse;
import com.training.starter.config.OpenApiSchemas;
import com.training.starter.dto.request.CreateProductRequest;
import com.training.starter.dto.request.UpdateProductRequest;
import com.training.starter.dto.response.ProductResponse;
import com.training.starter.dto.response.ProductSummaryResponse;
import com.training.starter.service.ProductService;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "listProducts", summary = "List products",
            description = "Returns a zero-based page of products. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ProductPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination or sort input",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<ProductSummaryResponse>> getAll(
            @Parameter(description = "Zero-based page index", example = "0",
                    schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of products per page", example = "20",
                    schema = @Schema(defaultValue = "20", minimum = "1"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Product property used for sorting", example = "createdAt",
                    schema = @Schema(defaultValue = "createdAt"))
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "DESC",
                    schema = @Schema(defaultValue = "DESC", allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        var result = productService.getAll(PageRequest.of(page, size, sort));
        return ApiResponse.success(PageResponse.from(result, r -> r));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "getProduct", summary = "Get a product",
            description = "Returns one product by ID. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ProductResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<ProductResponse> getById(
            @Parameter(description = "Product ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(productService.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "searchProducts", summary = "Search products",
            description = "Full-text search over product SKU and name with zero-based pagination. "
                    + "Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching products returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ProductPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Blank query, invalid pagination/direction, or unsupported search sort property",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<ProductSummaryResponse>> search(
            @Parameter(description = "Required, non-blank SKU or product-name search text", required = true,
                    example = "wireless mouse", schema = @Schema(minLength = 1))
            @RequestParam String q,
            @Parameter(description = "Zero-based page index", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of products per page", schema = @Schema(defaultValue = "20", minimum = "1"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Supported product property used for sorting",
                    schema = @Schema(defaultValue = "createdAt", allowableValues = {"id", "sku", "name", "description", "categoryId", "unit", "minStock", "maxStock", "reorderPoint", "reorderQuantity", "active", "createdAt", "updatedAt"}))
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", schema = @Schema(defaultValue = "DESC", allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        var result = productService.search(q, PageRequest.of(page, size, sort));
        return ApiResponse.success(PageResponse.from(result, r -> r));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createProduct", summary = "Create a product",
            description = "Creates a product. SKU is required and becomes immutable. Allowed roles: MANAGER or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ProductResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation or invalid related data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product SKU already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success("Product created", productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(operationId = "updateProduct", summary = "Update a product",
            description = "Updates mutable product fields; SKU is not an update property. Allowed roles: MANAGER or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ProductResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation or invalid related data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<ProductResponse> update(
            @Parameter(description = "Product ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success("Product updated", productService.update(id, request));
    }
}
