package com.training.starter.service;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateMovementItemRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockMovement;
import com.training.starter.entity.StockMovementItem;
import com.training.starter.entity.User;
import com.training.starter.entity.Warehouse;
import com.training.starter.enums.MovementStatus;
import com.training.starter.enums.MovementType;
import com.training.starter.exception.BadRequestException;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.StockMovementMapper;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.StockMovementItemRepository;
import com.training.starter.repository.StockMovementRepository;
import com.training.starter.repository.UserRepository;
import com.training.starter.repository.WarehouseRepository;
import com.training.starter.service.impl.StockMovementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    private static final String CURRENT_USERNAME = "staff";
    private static final Long CURRENT_USER_ID = 7L;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private StockMovementItemRepository stockMovementItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockMovementMapper stockMovementMapper;

    @InjectMocks
    private StockMovementServiceImpl stockMovementService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CURRENT_USERNAME, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Create: happy paths ----------

    @Test
    void createImport_validRequest_persistsMovementAndItems() {
        // Given
        stubValidCreateCollaborators();
        var request = importRequest(1L, item(10L, 5));

        // When
        stockMovementService.createImport(request);

        // Then
        StockMovement saved = captureSavedMovement();
        assertThat(saved.getType()).isEqualTo(MovementType.IMPORT);
        assertThat(saved.getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
        assertThat(saved.getWarehouseId()).isEqualTo(1L);
        assertThat(saved.getDestWarehouseId()).isNull();
        assertThat(saved.getReferenceNo()).startsWith("IMP-");
        assertThat(saved.getCreatedBy()).isEqualTo(CURRENT_USER_ID);

        List<StockMovementItem> savedItems = captureSavedItems();
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getMovementId()).isEqualTo(100L);
        assertThat(savedItems.get(0).getProductId()).isEqualTo(10L);
        assertThat(savedItems.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void createExport_validRequest_persistsMovement() {
        // Given
        stubValidCreateCollaborators();
        var request = new CreateExportRequest(1L, "ship out", List.of(item(10L, 3)));

        // When
        stockMovementService.createExport(request);

        // Then
        StockMovement saved = captureSavedMovement();
        assertThat(saved.getType()).isEqualTo(MovementType.EXPORT);
        assertThat(saved.getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
        assertThat(saved.getDestWarehouseId()).isNull();
        assertThat(saved.getReferenceNo()).startsWith("EXP-");
        verify(stockMovementItemRepository).saveAll(anyList());
    }

    @Test
    void createTransfer_validRequest_persistsWithDestination() {
        // Given
        stubValidCreateCollaborators();
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse(2L, "WH-2", "Second")));
        var request = new CreateTransferRequest(1L, 2L, "move", List.of(item(10L, 4)));

        // When
        stockMovementService.createTransfer(request);

        // Then
        StockMovement saved = captureSavedMovement();
        assertThat(saved.getType()).isEqualTo(MovementType.TRANSFER);
        assertThat(saved.getWarehouseId()).isEqualTo(1L);
        assertThat(saved.getDestWarehouseId()).isEqualTo(2L);
        assertThat(saved.getReferenceNo()).startsWith("TRF-");
    }

    @Test
    void createImport_validRequest_setsInitialStatusToPendingApproval() {
        // Given
        stubValidCreateCollaborators();

        // When
        stockMovementService.createImport(importRequest(1L, item(10L, 5)));

        // Then
        assertThat(captureSavedMovement().getStatus()).isEqualTo(MovementStatus.PENDING_APPROVAL);
    }

    @Test
    void createImport_validRequest_storesCreatedByFromAuthenticatedPrincipal() {
        // Given
        stubValidCreateCollaborators();

        // When
        stockMovementService.createImport(importRequest(1L, item(10L, 5)));

        // Then
        assertThat(captureSavedMovement().getCreatedBy()).isEqualTo(CURRENT_USER_ID);
    }

    // ---------- Create: guard clauses ----------

    @Test
    void createImport_unknownWarehouse_throwsResourceNotFoundException() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createImport(importRequest(1L, item(10L, 5))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createImport_unknownProduct_throwsResourceNotFoundException() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createImport(importRequest(1L, item(10L, 5))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createImport_nonPositiveQuantity_throwsBadRequestException() {
        // When & Then - validation happens before any repository call
        assertThatThrownBy(() -> stockMovementService.createImport(importRequest(1L, item(10L, 0))))
                .isInstanceOf(BadRequestException.class);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createImport_duplicateProductLine_throwsBadRequestException() {
        // Given - same product appears on two lines
        var request = importRequest(1L, item(10L, 5), item(10L, 2));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createImport(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate product line");
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createImport_invalidProduct_abortsBeforePersistingAnything() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of());

        // When & Then - proves creation is atomic: no movement and no items are saved
        assertThatThrownBy(() -> stockMovementService.createImport(importRequest(1L, item(10L, 5))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(stockMovementRepository, never()).save(any());
        verify(stockMovementItemRepository, never()).saveAll(any());
    }

    @Test
    void createTransfer_sameSourceAndDestination_throwsBadRequestException() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        var request = new CreateTransferRequest(1L, 1L, null, List.of(item(10L, 5)));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be different");
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createTransfer_missingDestination_throwsBadRequestException() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        var request = new CreateTransferRequest(1L, null, null, List.of(item(10L, 5)));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createTransfer(request))
                .isInstanceOf(BadRequestException.class);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createImport_referenceAlwaysColliding_throwsDuplicateResourceException() {
        // Given - every generated reference already exists
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "SKU-10", "Widget")));
        when(userRepository.findByUsername(CURRENT_USERNAME))
                .thenReturn(Optional.of(user(CURRENT_USER_ID, CURRENT_USERNAME)));
        when(stockMovementRepository.existsByReferenceNo(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> stockMovementService.createImport(importRequest(1L, item(10L, 5))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(stockMovementRepository, never()).save(any());
    }

    // ---------- Inspect ----------

    @Test
    void getById_found_returnsMovementResponse() {
        // Given
        StockMovement movement = StockMovement.builder()
                .referenceNo("IMP-20260724-1A2B")
                .type(MovementType.IMPORT)
                .status(MovementStatus.PENDING_APPROVAL)
                .warehouseId(1L)
                .createdBy(CURRENT_USER_ID)
                .build();
        movement.setId(5L);
        StockMovementItem storedItem = StockMovementItem.builder()
                .movementId(5L)
                .productId(10L)
                .quantity(5)
                .build();
        storedItem.setId(50L);
        MovementResponse expected = sampleResponse();

        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(stockMovementItemRepository.findByMovementId(5L)).thenReturn(List.of(storedItem));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "SKU-10", "Widget")));
        when(stockMovementMapper.toResponse(any(), any(), any(), any())).thenReturn(expected);

        // When
        MovementResponse result = stockMovementService.getById(5L);

        // Then
        assertThat(result).isSameAs(expected);
        verify(stockMovementItemRepository).findByMovementId(5L);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        // Given
        when(stockMovementRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockMovementService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- Helpers ----------

    private void stubValidCreateCollaborators() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "SKU-10", "Widget")));
        when(userRepository.findByUsername(CURRENT_USERNAME))
                .thenReturn(Optional.of(user(CURRENT_USER_ID, CURRENT_USERNAME)));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setId(100L);
            return movement;
        });
        when(stockMovementItemRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StockMovement captureSavedMovement() {
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<StockMovementItem> captureSavedItems() {
        ArgumentCaptor<List<StockMovementItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockMovementItemRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private CreateImportRequest importRequest(Long warehouseId, CreateMovementItemRequest... items) {
        return new CreateImportRequest(warehouseId, "notes", List.of(items));
    }

    private CreateMovementItemRequest item(Long productId, int quantity) {
        return new CreateMovementItemRequest(productId, quantity, null, null, null, null);
    }

    private Product product(Long id, String sku, String name) {
        Product product = Product.builder().sku(sku).name(name).build();
        product.setId(id);
        return product;
    }

    private Warehouse warehouse(Long id, String code, String name) {
        Warehouse warehouse = Warehouse.builder().code(code).name(name).build();
        warehouse.setId(id);
        return warehouse;
    }

    private User user(Long id, String username) {
        User user = User.builder().username(username).build();
        user.setId(id);
        return user;
    }

    private MovementResponse sampleResponse() {
        return new MovementResponse(
                5L, "IMP-20260724-1A2B", "IMPORT", "PENDING_APPROVAL",
                1L, "WH-1", "Main", null, null, null,
                null, CURRENT_USER_ID, null, List.of(), null);
    }
}
