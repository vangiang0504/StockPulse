package com.training.starter.controller;

import com.training.starter.common.ApiResponse;
import com.training.starter.common.PageResponse;
import com.training.starter.config.OpenApiSchemas;
import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.dto.response.StockSummaryResponse;
import com.training.starter.exception.BadRequestException;
import com.training.starter.service.StockLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Transactional stock-level and reporting queries")
@SecurityRequirement(name = "Bearer Authentication")
public class StockController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_PROPERTIES = Set.of(
            "id",
            "productId",
            "warehouseId",
            "quantity",
            "reservedQuantity",
            "version",
            "updatedAt");
    private static final Map<String, String> SUMMARY_SORT_COLUMNS = Map.ofEntries(
            Map.entry("productId", "product_id"),
            Map.entry("sku", "sku"),
            Map.entry("productName", "product_name"),
            Map.entry("categoryName", "category_name"),
            Map.entry("warehouseId", "warehouse_id"),
            Map.entry("warehouseName", "warehouse_name"),
            Map.entry("quantity", "quantity"),
            Map.entry("reservedQuantity", "reserved_quantity"),
            Map.entry("availableQuantity", "available_quantity"),
            Map.entry("minStock", "min_stock"),
            Map.entry("reorderPoint", "reorder_point"),
            Map.entry("stockStatus", "stock_status"));

    private final StockLevelService stockLevelService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(
            operationId = "listStockLevels",
            summary = "List stock levels",
            description = "Returns a zero-based page of transactional stock levels, optionally "
                    + "filtered by Warehouse and Product. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock levels returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.StockLevelPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter, pagination, or sort input",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid authentication",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<StockLevelResponse>> getAll(
            @Parameter(description = "Optional Warehouse ID filter", example = "1")
            @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Optional Product ID filter", example = "1")
            @RequestParam(required = false) Long productId,
            @Parameter(
                    description = "Zero-based page index",
                    schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(
                    description = "Number of stock levels per page",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(
                    description = "StockLevel property used for sorting",
                    schema = @Schema(
                            defaultValue = "updatedAt",
                            allowableValues = {
                                    "id",
                                    "productId",
                                    "warehouseId",
                                    "quantity",
                                    "reservedQuantity",
                                    "version",
                                    "updatedAt"
                            }))
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @Parameter(
                    description = "Sort direction",
                    schema = @Schema(
                            defaultValue = "DESC",
                            allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "DESC") String sortDir) {
        validateFilterId(warehouseId, "Warehouse");
        validateFilterId(productId, "Product");
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        var result = stockLevelService.getAll(warehouseId, productId, pageable);
        return ApiResponse.success(PageResponse.from(result, response -> response));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(
            operationId = "listLowStockLevels",
            summary = "List low-stock levels",
            description = "Returns a zero-based page of stock levels whose quantity is at or "
                    + "below the Product-specific reorder point, optionally filtered by Warehouse. "
                    + "Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Low-stock levels returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.StockLevelPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter, pagination, or sort input",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid authentication",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<StockLevelResponse>> getLowStock(
            @Parameter(description = "Optional Warehouse ID filter", example = "1")
            @RequestParam(required = false) Long warehouseId,
            @Parameter(
                    description = "Zero-based page index",
                    schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(
                    description = "Number of low-stock levels per page",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(
                    description = "StockLevel property used for sorting",
                    schema = @Schema(
                            defaultValue = "updatedAt",
                            allowableValues = {
                                    "id",
                                    "productId",
                                    "warehouseId",
                                    "quantity",
                                    "reservedQuantity",
                                    "version",
                                    "updatedAt"
                            }))
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @Parameter(
                    description = "Sort direction",
                    schema = @Schema(
                            defaultValue = "DESC",
                            allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "DESC") String sortDir) {
        validateFilterId(warehouseId, "Warehouse");
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        var result = stockLevelService.getLowStock(warehouseId, pageable);
        return ApiResponse.success(PageResponse.from(result, response -> response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(
            operationId = "listStockSummary",
            summary = "List stock summary",
            description = "Returns a zero-based page sourced from the stock summary materialized "
                    + "view, optionally filtered by Warehouse and Product. Allowed roles: STAFF, "
                    + "MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock summary returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.StockSummaryPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter, pagination, or sort input",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid authentication",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Insufficient role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<StockSummaryResponse>> getSummary(
            @Parameter(description = "Optional Warehouse ID filter", example = "1")
            @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Optional Product ID filter", example = "1")
            @RequestParam(required = false) Long productId,
            @Parameter(
                    description = "Zero-based page index",
                    schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(
                    description = "Number of summary rows per page",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(
                    description = "StockSummary property used for sorting",
                    schema = @Schema(
                            defaultValue = "productId",
                            allowableValues = {
                                    "productId",
                                    "sku",
                                    "productName",
                                    "categoryName",
                                    "warehouseId",
                                    "warehouseName",
                                    "quantity",
                                    "reservedQuantity",
                                    "availableQuantity",
                                    "minStock",
                                    "reorderPoint",
                                    "stockStatus"
                            }))
            @RequestParam(defaultValue = "productId") String sortBy,
            @Parameter(
                    description = "Sort direction",
                    schema = @Schema(
                            defaultValue = "DESC",
                            allowableValues = {"ASC", "DESC"}))
            @RequestParam(defaultValue = "DESC") String sortDir) {
        validateFilterId(warehouseId, "Warehouse");
        validateFilterId(productId, "Product");
        Pageable pageable = createSummaryPageable(page, size, sortBy, sortDir);
        var result = stockLevelService.getSummary(warehouseId, productId, pageable);
        return ApiResponse.success(PageResponse.from(result, response -> response));
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        validatePagination(page, size);
        if (!SORT_PROPERTIES.contains(sortBy)) {
            throw new BadRequestException("Unsupported stock sort property: " + sortBy);
        }
        return PageRequest.of(page, size, Sort.by(parseSortDirection(sortDir), sortBy));
    }

    private Pageable createSummaryPageable(
            int page,
            int size,
            String sortBy,
            String sortDir) {
        validatePagination(page, size);
        String column = SUMMARY_SORT_COLUMNS.get(sortBy);
        if (column == null) {
            throw new BadRequestException("Unsupported stock summary sort property: " + sortBy);
        }
        return PageRequest.of(page, size, Sort.by(parseSortDirection(sortDir), column));
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Sort direction must be ASC or DESC");
        }
    }

    private void validateFilterId(Long id, String resourceName) {
        if (id != null && id < 1) {
            throw new BadRequestException(resourceName + " ID must be positive");
        }
    }
}
