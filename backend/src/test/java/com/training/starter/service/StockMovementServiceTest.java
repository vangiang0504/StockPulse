package com.training.starter.service;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateMovementItemRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
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
import com.training.starter.messaging.StockEventPublisher;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.StockLevelRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private StockLevelRepository stockLevelRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockMovementMapper stockMovementMapper;

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private StockEventPublisher stockEventPublisher;

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

    @Test
    void getAll_filtersAndPageable_returnsMappedMovementPage() {
        // Given
        StockMovement movement = pendingMovement();
        StockMovementItem item = storedItem(10L, 5);
        MovementResponse expected = sampleResponse();
        PageRequest pageable = PageRequest.of(0, 20);
        when(stockMovementRepository.findAllWithFilters(
                        MovementType.IMPORT,
                        MovementStatus.PENDING_APPROVAL,
                        1L,
                        pageable))
                .thenReturn(new PageImpl<>(List.of(movement), pageable, 1));
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(stockMovementItemRepository.findByMovementId(5L)).thenReturn(List.of(item));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "SKU-10", "Widget")));
        when(stockMovementMapper.toResponse(any(), any(), any(), any())).thenReturn(expected);

        // When
        var result = stockMovementService.getAll(
                MovementType.IMPORT,
                MovementStatus.PENDING_APPROVAL,
                1L,
                pageable);

        // Then
        assertThat(result.getContent()).containsExactly(expected);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(stockMovementRepository).findAllWithFilters(
                MovementType.IMPORT,
                MovementStatus.PENDING_APPROVAL,
                1L,
                pageable);
    }

    // ---------- Approve movement ----------

    @Test
    void approve_pendingApproval_setsApproverAndApprovedStatus() {
        // Given
        StockMovement movement = pendingMovement();
        StockMovementItem item = storedItem(10L, 5);
        MovementResponse expected = sampleResponse();
        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));
        when(userRepository.findByUsername(CURRENT_USERNAME))
                .thenReturn(Optional.of(user(CURRENT_USER_ID, CURRENT_USERNAME)));
        when(stockMovementRepository.save(movement)).thenReturn(movement);
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        when(stockMovementItemRepository.findByMovementId(5L)).thenReturn(List.of(item));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "SKU-10", "Widget")));
        when(stockMovementMapper.toResponse(any(), any(), any(), any())).thenReturn(expected);

        // When
        MovementResponse result = stockMovementService.approve(5L);

        // Then
        assertThat(result).isSameAs(expected);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.APPROVED);
        assertThat(movement.getApprovedBy()).isEqualTo(CURRENT_USER_ID);
        verify(stockMovementRepository).save(movement);
        verify(stockLevelRepository, never()).saveAll(anyList());
    }

    @Test
    void approve_notPendingApproval_throwsBadRequestWithoutMutation() {
        // Given
        StockMovement movement = pendingMovement();
        movement.setStatus(MovementStatus.COMPLETED);
        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.approve(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING_APPROVAL");
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.COMPLETED);
        assertThat(movement.getApprovedBy()).isNull();
        verify(userRepository, never()).findByUsername(anyString());
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void approve_notFound_throwsResourceNotFoundException() {
        // Given
        when(stockMovementRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> stockMovementService.approve(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    // ---------- Complete movement ----------

    @Test
    void completeMovement_import_increasesStock() {
        // Given
        StockMovement movement = approvedMovement(MovementType.IMPORT, 1L, null);
        StockMovementItem item = storedItem(10L, 5);
        StockLevel level = stockLevel(1L, 10L, 8);
        stubCompletionCollaborators(movement, List.of(item), List.of(level));

        // When
        stockMovementService.completeMovement(5L);

        // Then
        assertThat(level.getQuantity()).isEqualTo(13);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.COMPLETED);
        verify(stockLevelRepository).findAllForUpdate(List.of(1L), List.of(10L));
        verify(stockLevelRepository).saveAll(anyList());
        verify(stockMovementRepository).save(movement);
        verify(stockCacheService).evictAfterCommit(
                java.util.Set.of(new StockCacheKey(1L, 10L)));
        ArgumentCaptor<StockMovementCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(StockMovementCompletedEvent.class);
        verify(stockEventPublisher).publishMovementCompleted(eventCaptor.capture());
        assertThat(eventCaptor.getValue().movementId()).isEqualTo(5L);
        assertThat(eventCaptor.getValue().movementReference())
                .isEqualTo("MOV-20260724-1A2B");
        assertThat(eventCaptor.getValue().movementType()).isEqualTo(MovementType.IMPORT);
        assertThat(eventCaptor.getValue().productIds()).containsExactly(10L);
        assertThat(eventCaptor.getValue().warehouseIds()).containsExactly(1L);
    }

    @Test
    void completeMovement_importWithoutExistingLevel_createsStockLevel() {
        // Given
        StockMovement movement = approvedMovement(MovementType.IMPORT, 1L, null);
        stubCompletionCollaborators(
                movement, List.of(storedItem(10L, 5)), List.of());

        // When
        stockMovementService.completeMovement(5L);

        // Then
        List<StockLevel> savedLevels = captureSavedStockLevels();
        assertThat(savedLevels).singleElement().satisfies(level -> {
            assertThat(level.getWarehouseId()).isEqualTo(1L);
            assertThat(level.getProductId()).isEqualTo(10L);
            assertThat(level.getQuantity()).isEqualTo(5);
            assertThat(level.getReservedQuantity()).isZero();
        });
    }

    @Test
    void completeMovement_export_decreasesStock() {
        // Given
        StockMovement movement = approvedMovement(MovementType.EXPORT, 1L, null);
        StockLevel level = stockLevel(1L, 10L, 8);
        stubCompletionCollaborators(
                movement, List.of(storedItem(10L, 3)), List.of(level));

        // When
        stockMovementService.completeMovement(5L);

        // Then
        assertThat(level.getQuantity()).isEqualTo(5);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.COMPLETED);
    }

    @Test
    void completeMovement_exportWithInsufficientStock_throwsBadRequestWithoutMutation() {
        // Given
        StockMovement movement = approvedMovement(MovementType.EXPORT, 1L, null);
        StockLevel level = stockLevel(1L, 10L, 2);
        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));
        when(stockMovementItemRepository.findByMovementId(5L))
                .thenReturn(List.of(storedItem(10L, 3)));
        when(stockLevelRepository.findAllForUpdate(List.of(1L), List.of(10L)))
                .thenReturn(List.of(level));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.completeMovement(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("10");
        assertThat(level.getQuantity()).isEqualTo(2);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.APPROVED);
        verify(stockLevelRepository, never()).saveAll(anyList());
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
        verify(stockCacheService, never()).evictAfterCommit(any());
        verify(stockEventPublisher, never())
                .publishMovementCompleted(any(StockMovementCompletedEvent.class));
    }

    @Test
    void completeMovement_transfer_updatesSourceAndDestination() {
        // Given
        StockMovement movement = approvedMovement(MovementType.TRANSFER, 1L, 2L);
        StockLevel source = stockLevel(1L, 10L, 9);
        StockLevel destination = stockLevel(2L, 10L, 4);
        stubCompletionCollaborators(
                movement,
                List.of(storedItem(10L, 3)),
                List.of(source, destination));

        // When
        stockMovementService.completeMovement(5L);

        // Then
        assertThat(source.getQuantity()).isEqualTo(6);
        assertThat(destination.getQuantity()).isEqualTo(7);
        verify(stockLevelRepository).findAllForUpdate(List.of(1L, 2L), List.of(10L));
        verify(stockCacheService).evictAfterCommit(java.util.Set.of(
                new StockCacheKey(1L, 10L),
                new StockCacheKey(2L, 10L)));
    }

    @Test
    void completeMovement_multipleProducts_locksInProductIdOrder() {
        // Given
        StockMovement movement = approvedMovement(MovementType.IMPORT, 1L, null);
        List<StockMovementItem> items =
                List.of(storedItem(20L, 2), storedItem(10L, 1));
        stubCompletionCollaborators(movement, items, List.of());

        // When
        stockMovementService.completeMovement(5L);

        // Then
        verify(stockLevelRepository).findAllForUpdate(List.of(1L), List.of(10L, 20L));
        assertThat(captureSavedStockLevels())
                .extracting(StockLevel::getProductId)
                .containsExactly(10L, 20L);
    }

    @Test
    void completeMovement_notApproved_throwsBadRequestBeforeLockingStock() {
        // Given
        StockMovement movement = approvedMovement(MovementType.IMPORT, 1L, null);
        movement.setStatus(MovementStatus.PENDING_APPROVAL);
        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));

        // When & Then
        assertThatThrownBy(() -> stockMovementService.completeMovement(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED");
        verify(stockMovementItemRepository, never()).findByMovementId(any());
        verify(stockLevelRepository, never()).findAllForUpdate(anyList(), anyList());
    }

    // ---------- Helpers ----------

    private void stubCompletionCollaborators(
            StockMovement movement,
            List<StockMovementItem> items,
            List<StockLevel> levels) {
        when(stockMovementRepository.findById(5L)).thenReturn(Optional.of(movement));
        when(stockMovementItemRepository.findByMovementId(5L)).thenReturn(items);
        when(stockLevelRepository.findAllForUpdate(anyList(), anyList())).thenReturn(levels);
        when(stockMovementRepository.save(movement)).thenReturn(movement);
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse(1L, "WH-1", "Main")));
        if (movement.getDestWarehouseId() != null) {
            when(warehouseRepository.findById(2L))
                    .thenReturn(Optional.of(warehouse(2L, "WH-2", "Second")));
        }
        when(productRepository.findAllById(anyIterable())).thenReturn(items.stream()
                .map(StockMovementItem::getProductId)
                .distinct()
                .map(productId -> product(productId, "SKU-" + productId, "Product " + productId))
                .toList());
    }

    private StockMovement approvedMovement(
            MovementType type, Long warehouseId, Long destWarehouseId) {
        StockMovement movement = StockMovement.builder()
                .referenceNo("MOV-20260724-1A2B")
                .type(type)
                .status(MovementStatus.APPROVED)
                .warehouseId(warehouseId)
                .destWarehouseId(destWarehouseId)
                .createdBy(CURRENT_USER_ID)
                .build();
        movement.setId(5L);
        return movement;
    }

    private StockMovement pendingMovement() {
        StockMovement movement = StockMovement.builder()
                .referenceNo("IMP-20260724-1A2B")
                .type(MovementType.IMPORT)
                .status(MovementStatus.PENDING_APPROVAL)
                .warehouseId(1L)
                .createdBy(CURRENT_USER_ID)
                .build();
        movement.setId(5L);
        return movement;
    }

    private StockMovementItem storedItem(Long productId, int quantity) {
        StockMovementItem item = StockMovementItem.builder()
                .movementId(5L)
                .productId(productId)
                .quantity(quantity)
                .build();
        item.setId(productId + 100L);
        return item;
    }

    private StockLevel stockLevel(Long warehouseId, Long productId, int quantity) {
        return StockLevel.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(quantity)
                .reservedQuantity(0)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<StockLevel> captureSavedStockLevels() {
        ArgumentCaptor<List<StockLevel>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockLevelRepository).saveAll(captor.capture());
        return captor.getValue();
    }

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
