package com.training.starter.controller;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.dto.response.StockSummaryResponse;
import com.training.starter.enums.StockStatus;
import com.training.starter.exception.GlobalExceptionHandler;
import com.training.starter.service.StockLevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    private StockLevelService stockLevelService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        stockLevelService = mock(StockLevelService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StockController(stockLevelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAll_validFilters_returnsEstablishedPageEnvelope() throws Exception {
        // Given
        StockLevelResponse response = new StockLevelResponse(
                7L,
                11L,
                "SKU-011",
                "Mapped product",
                22L,
                "WH-022",
                "Mapped warehouse",
                40,
                7,
                33,
                20,
                3L,
                LocalDateTime.of(2026, 7, 23, 10, 0));
        when(stockLevelService.getAll(
                org.mockito.ArgumentMatchers.eq(22L),
                org.mockito.ArgumentMatchers.eq(11L),
                any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(2);
                    return new PageImpl<>(List.of(response), pageable, 3);
                });

        // When & Then
        mockMvc.perform(get("/api/v1/stock")
                        .param("warehouseId", "22")
                        .param("productId", "11")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "updatedAt")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productSku").value("SKU-011"))
                .andExpect(jsonPath("$.data.content[0].warehouseCode").value("WH-022"))
                .andExpect(jsonPath("$.data.content[0].availableQuantity").value(33))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockLevelService).getAll(
                org.mockito.ArgumentMatchers.eq(22L),
                org.mockito.ArgumentMatchers.eq(11L),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(2);
        assertThat(pageable.getSort().getOrderFor("updatedAt"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAll_unsupportedSort_returnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(get("/api/v1/stock").param("sortBy", "productName"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Unsupported stock sort property: productName"));

        verifyNoInteractions(stockLevelService);
    }

    @Test
    void getAll_invalidPaginationOrFilters_returnsBadRequestWithoutServiceCall() throws Exception {
        String[][] invalidParameters = {
                {"page", "-1"},
                {"size", "0"},
                {"size", "101"},
                {"warehouseId", "0"},
                {"productId", "-1"},
                {"sortDir", "SIDEWAYS"}
        };

        for (String[] parameter : invalidParameters) {
            mockMvc.perform(get("/api/v1/stock").param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        verify(stockLevelService, never()).getAll(any(), any(), any());
    }

    @Test
    void getAll_declaresAcceptedReadRoleMatrix() throws Exception {
        PreAuthorize authorization = StockController.class
                .getMethod(
                        "getAll",
                        Long.class,
                        Long.class,
                        int.class,
                        int.class,
                        String.class,
                        String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value())
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
    }

    @Test
    void getLowStock_validRequest_returnsEstablishedPageEnvelope() throws Exception {
        // Given
        StockLevelResponse response = new StockLevelResponse(
                8L,
                12L,
                "SKU-012",
                "Low-stock product",
                22L,
                "WH-022",
                "Mapped warehouse",
                20,
                3,
                17,
                20,
                1L,
                LocalDateTime.of(2026, 7, 23, 11, 0));
        when(stockLevelService.getLowStock(
                org.mockito.ArgumentMatchers.eq(22L),
                any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<>(List.of(response), pageable, 1);
                });

        // When & Then
        mockMvc.perform(get("/api/v1/stock/low")
                        .param("warehouseId", "22")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "quantity")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productSku").value("SKU-012"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(20))
                .andExpect(jsonPath("$.data.content[0].reorderPoint").value(20))
                .andExpect(jsonPath("$.data.content[0].availableQuantity").value(17))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.last").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockLevelService).getLowStock(
                org.mockito.ArgumentMatchers.eq(22L),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("quantity"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getLowStock_invalidInput_returnsBadRequestWithoutServiceCall() throws Exception {
        String[][] invalidParameters = {
                {"warehouseId", "0"},
                {"page", "-1"},
                {"size", "101"},
                {"sortBy", "productName"},
                {"sortDir", "SIDEWAYS"}
        };

        for (String[] parameter : invalidParameters) {
            mockMvc.perform(get("/api/v1/stock/low").param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        verify(stockLevelService, never()).getLowStock(any(), any());
    }

    @Test
    void getLowStock_declaresAcceptedReadRoleMatrix() throws Exception {
        PreAuthorize authorization = StockController.class
                .getMethod(
                        "getLowStock",
                        Long.class,
                        int.class,
                        int.class,
                        String.class,
                        String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value())
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
    }

    @Test
    void getSummary_validFilters_returnsEstablishedPageEnvelopeAndTranslatesSort() throws Exception {
        // Given
        StockSummaryResponse response = new StockSummaryResponse(
                11L,
                "SKU-011",
                "Summary product",
                "Summary category",
                22L,
                "Summary warehouse",
                40,
                7,
                33,
                10,
                20,
                StockStatus.NORMAL);
        when(stockLevelService.getSummary(
                org.mockito.ArgumentMatchers.eq(22L),
                org.mockito.ArgumentMatchers.eq(11L),
                any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(2);
                    return new PageImpl<>(List.of(response), pageable, 3);
                });

        // When & Then
        mockMvc.perform(get("/api/v1/stock/summary")
                        .param("warehouseId", "22")
                        .param("productId", "11")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "stockStatus")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sku").value("SKU-011"))
                .andExpect(jsonPath("$.data.content[0].categoryName")
                        .value("Summary category"))
                .andExpect(jsonPath("$.data.content[0].availableQuantity").value(33))
                .andExpect(jsonPath("$.data.content[0].stockStatus").value("NORMAL"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockLevelService).getSummary(
                org.mockito.ArgumentMatchers.eq(22L),
                org.mockito.ArgumentMatchers.eq(11L),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(2);
        assertThat(pageable.getSort().getOrderFor("stock_status"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getSummary_invalidInput_returnsBadRequestWithoutServiceCall() throws Exception {
        String[][] invalidParameters = {
                {"warehouseId", "0"},
                {"productId", "-1"},
                {"page", "-1"},
                {"size", "0"},
                {"size", "101"},
                {"sortBy", "updatedAt"},
                {"sortDir", "SIDEWAYS"}
        };

        for (String[] parameter : invalidParameters) {
            mockMvc.perform(get("/api/v1/stock/summary")
                            .param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        verify(stockLevelService, never()).getSummary(any(), any(), any());
    }

    @Test
    void getSummary_declaresAcceptedReadRoleMatrix() throws Exception {
        PreAuthorize authorization = StockController.class
                .getMethod(
                        "getSummary",
                        Long.class,
                        Long.class,
                        int.class,
                        int.class,
                        String.class,
                        String.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value())
                .isEqualTo("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')");
    }
}
