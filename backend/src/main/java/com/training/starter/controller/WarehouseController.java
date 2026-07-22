package com.training.starter.controller;

import com.training.starter.common.ApiResponse;
import com.training.starter.common.PageResponse;
import com.training.starter.config.OpenApiSchemas;
import com.training.starter.dto.request.CreateWarehouseRequest;
import com.training.starter.dto.request.UpdateWarehouseRequest;
import com.training.starter.dto.response.WarehouseResponse;
import com.training.starter.service.WarehouseService;
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
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "listWarehouses", summary = "List warehouses",
            description = "Returns a zero-based page of warehouses. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouses returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.WarehousePageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination or sort input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<WarehouseResponse>> getAll(
            @Parameter(description = "Zero-based page index", schema = @Schema(defaultValue = "0", minimum = "0")) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of warehouses per page", schema = @Schema(defaultValue = "20", minimum = "1")) @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Warehouse property used for sorting", schema = @Schema(defaultValue = "createdAt")) @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", schema = @Schema(defaultValue = "DESC", allowableValues = {"ASC", "DESC"})) @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        var result = warehouseService.getAll(PageRequest.of(page, size, sort));
        return ApiResponse.success(PageResponse.from(result, r -> r));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(operationId = "getWarehouse", summary = "Get a warehouse",
            description = "Returns one warehouse by ID. Allowed roles: STAFF, MANAGER, or ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.WarehouseResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<WarehouseResponse> getById(@Parameter(description = "Warehouse ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(warehouseService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createWarehouse", summary = "Create a warehouse",
            description = "Creates a warehouse. Code is required and becomes immutable. Allowed role: ADMIN only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Warehouse created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.WarehouseResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Warehouse code already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.success("Warehouse created", warehouseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updateWarehouse", summary = "Update a warehouse",
            description = "Updates mutable warehouse fields; code is not an update property. Allowed role: ADMIN only.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Warehouse updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.WarehouseResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<WarehouseResponse> update(
            @Parameter(description = "Warehouse ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.success("Warehouse updated", warehouseService.update(id, request));
    }
}
