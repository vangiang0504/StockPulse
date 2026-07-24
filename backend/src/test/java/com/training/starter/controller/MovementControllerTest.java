package com.training.starter.controller;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.enums.MovementStatus;
import com.training.starter.enums.MovementType;
import com.training.starter.exception.GlobalExceptionHandler;
import com.training.starter.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovementControllerTest {

    private StockMovementService stockMovementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        stockMovementService = mock(StockMovementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MovementController(stockMovementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createImport_validRequest_returnsCreatedEnvelope() throws Exception {
        MovementResponse response = movementResponse("IMPORT", "PENDING_APPROVAL");
        when(stockMovementService.createImport(any(CreateImportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/movements/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 1,
                                  "notes": "Inbound shipment",
                                  "items": [{"productId": 10, "quantity": 5}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("IMPORT"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        verify(stockMovementService).createImport(any(CreateImportRequest.class));
    }

    @Test
    void createExport_validRequest_returnsCreatedEnvelope() throws Exception {
        when(stockMovementService.createExport(any(CreateExportRequest.class)))
                .thenReturn(movementResponse("EXPORT", "PENDING_APPROVAL"));

        mockMvc.perform(post("/api/v1/movements/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 1,
                                  "items": [{"productId": 10, "quantity": 3}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("EXPORT"));
    }

    @Test
    void createTransfer_validRequest_returnsCreatedEnvelope() throws Exception {
        when(stockMovementService.createTransfer(any(CreateTransferRequest.class)))
                .thenReturn(movementResponse("TRANSFER", "PENDING_APPROVAL"));

        mockMvc.perform(post("/api/v1/movements/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 1,
                                  "destWarehouseId": 2,
                                  "items": [{"productId": 10, "quantity": 3}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("TRANSFER"));
    }

    @Test
    void createImport_invalidNestedItem_returnsValidationErrorWithoutServiceCall()
            throws Exception {
        mockMvc.perform(post("/api/v1/movements/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "warehouseId": 0,
                                  "items": [{"productId": -1, "quantity": 0}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.warehouseId")
                        .value("Warehouse ID must be positive"))
                .andExpect(jsonPath("$.data['items[0].productId']")
                        .value("Product ID must be positive"))
                .andExpect(jsonPath("$.data['items[0].quantity']")
                        .value("Quantity must be positive"));

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void getAll_validFilters_returnsPageEnvelopeAndDelegatesParsedFilters()
            throws Exception {
        MovementResponse response = movementResponse("IMPORT", "APPROVED");
        when(stockMovementService.getAll(
                        eq(MovementType.IMPORT),
                        eq(MovementStatus.APPROVED),
                        eq(1L),
                        any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(3);
                    return new PageImpl<>(List.of(response), pageable, 3);
                });

        mockMvc.perform(get("/api/v1/movements")
                        .param("type", "import")
                        .param("status", "approved")
                        .param("warehouseId", "1")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "referenceNo")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].referenceNo")
                        .value("IMP-20260724-0001"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockMovementService).getAll(
                eq(MovementType.IMPORT),
                eq(MovementStatus.APPROVED),
                eq(1L),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(2);
        assertThat(pageable.getSort().getOrderFor("referenceNo"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAll_invalidType_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/movements").param("type", "PURCHASE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Unsupported movement type: PURCHASE"));

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void getAll_invalidPagination_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/movements").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Page size must be between 1 and 100"));

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void getById_validId_returnsMovement() throws Exception {
        when(stockMovementService.getById(5L))
                .thenReturn(movementResponse("IMPORT", "PENDING_APPROVAL"));

        mockMvc.perform(get("/api/v1/movements/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.referenceNo")
                        .value("IMP-20260724-0001"));
    }

    @Test
    void approve_validId_returnsApprovedMovement() throws Exception {
        when(stockMovementService.approve(5L))
                .thenReturn(movementResponse("IMPORT", "APPROVED"));

        mockMvc.perform(put("/api/v1/movements/5/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(stockMovementService).approve(5L);
    }

    @Test
    void complete_validId_returnsCompletedMovement() throws Exception {
        when(stockMovementService.completeMovement(5L))
                .thenReturn(movementResponse("IMPORT", "COMPLETED"));

        mockMvc.perform(put("/api/v1/movements/5/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(stockMovementService).completeMovement(5L);
    }

    @Test
    void lifecycle_invalidId_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(put("/api/v1/movements/0/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Movement ID must be positive"));

        verify(stockMovementService, never()).completeMovement(any());
    }

    @Test
    void endpoints_declareRequiredRoleMatrix() throws Exception {
        assertThat(roleFor(
                        "createImport", CreateImportRequest.class))
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
        assertThat(roleFor("getById", Long.class))
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
        assertThat(roleFor("approve", Long.class))
                .isEqualTo("hasAnyRole('MANAGER', 'ADMIN')");
        assertThat(roleFor("complete", Long.class))
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
    }

    private String roleFor(String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        PreAuthorize annotation = MovementController.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        return annotation.value();
    }

    private MovementResponse movementResponse(String type, String status) {
        return new MovementResponse(
                5L,
                "IMP-20260724-0001",
                type,
                status,
                1L,
                "WH-001",
                "Main Warehouse",
                "TRANSFER".equals(type) ? 2L : null,
                "TRANSFER".equals(type) ? "WH-002" : null,
                "TRANSFER".equals(type) ? "Second Warehouse" : null,
                "notes",
                7L,
                "APPROVED".equals(status) || "COMPLETED".equals(status) ? 8L : null,
                List.of(),
                LocalDateTime.of(2026, 7, 24, 10, 0));
    }
}
