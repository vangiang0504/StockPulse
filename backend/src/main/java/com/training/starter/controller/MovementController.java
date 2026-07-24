package com.training.starter.controller;

import com.training.starter.common.ApiResponse;
import com.training.starter.common.PageResponse;
import com.training.starter.config.OpenApiSchemas;
import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.enums.MovementStatus;
import com.training.starter.enums.MovementType;
import com.training.starter.exception.BadRequestException;
import com.training.starter.service.StockMovementService;
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
import org.springframework.data.domain.Pageable;
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

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Stock import, export, and transfer lifecycle")
@SecurityRequirement(name = "Bearer Authentication")
public class MovementController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_PROPERTIES = Set.of(
            "id",
            "referenceNo",
            "type",
            "status",
            "warehouseId",
            "destWarehouseId",
            "createdBy",
            "approvedBy",
            "createdAt",
            "updatedAt");

    private final StockMovementService stockMovementService;

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create an import movement")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Import movement created",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid movement data",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> createImport(
            @Valid @RequestBody CreateImportRequest request) {
        return ApiResponse.success(stockMovementService.createImport(request));
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create an export movement")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Export movement created",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid movement data",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> createExport(
            @Valid @RequestBody CreateExportRequest request) {
        return ApiResponse.success(stockMovementService.createExport(request));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create a transfer movement")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Transfer movement created",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid movement data",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {
        return ApiResponse.success(stockMovementService.createTransfer(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(
            summary = "List stock movements",
            description = "Supports optional type, status, and source-or-destination warehouse filters")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Movement page returned",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementPageResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter, pagination, or sort input",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<PageResponse<MovementResponse>> getAll(
            @Parameter(description = "Movement type: IMPORT, EXPORT, TRANSFER, or ADJUSTMENT")
            @RequestParam(required = false)
            String type,
            @Parameter(
                    description =
                            "Movement status, for example PENDING_APPROVAL, APPROVED, or COMPLETED")
            @RequestParam(required = false)
            String status,
            @Parameter(description = "Source or destination Warehouse ID")
            @RequestParam(required = false)
            Long warehouseId,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "20")
            int size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "DESC")
            String sortDir) {
        validateOptionalPositiveId(warehouseId, "Warehouse");
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        var result = stockMovementService.getAll(
                parseType(type), parseStatus(status), warehouseId, pageable);
        return ApiResponse.success(PageResponse.from(result, response -> response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get movement detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Movement returned",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Movement not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> getById(@PathVariable Long id) {
        validatePositiveId(id, "Movement");
        return ApiResponse.success(stockMovementService.getById(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Approve a pending movement")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Movement approved",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Movement cannot be approved from its current status",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> approve(@PathVariable Long id) {
        validatePositiveId(id, "Movement");
        return ApiResponse.success(stockMovementService.approve(id));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Complete an approved movement and update stock")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Movement completed",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            OpenApiSchemas.MovementResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Movement cannot be completed or stock is insufficient",
                    content = @Content(
                            schema = @Schema(
                                    implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<MovementResponse> complete(@PathVariable Long id) {
        validatePositiveId(id, "Movement");
        return ApiResponse.success(stockMovementService.completeMovement(id));
    }

    private Pageable createPageable(
            int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORT_PROPERTIES.contains(sortBy)) {
            throw new BadRequestException(
                    "Unsupported movement sort property: " + sortBy);
        }
        try {
            return PageRequest.of(
                    page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Sort direction must be ASC or DESC");
        }
    }

    private MovementType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return MovementType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Unsupported movement type: " + type);
        }
    }

    private MovementStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MovementStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Unsupported movement status: " + status);
        }
    }

    private void validatePositiveId(Long id, String resourceName) {
        if (id == null || id < 1) {
            throw new BadRequestException(resourceName + " ID must be positive");
        }
    }

    private void validateOptionalPositiveId(Long id, String resourceName) {
        if (id != null && id < 1) {
            throw new BadRequestException(resourceName + " ID must be positive");
        }
    }
}
