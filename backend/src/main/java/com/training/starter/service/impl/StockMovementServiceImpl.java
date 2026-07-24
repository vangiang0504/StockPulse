package com.training.starter.service.impl;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateMovementItemRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementItemResponse;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.entity.Product;
import com.training.starter.entity.StockMovement;
import com.training.starter.entity.StockMovementItem;
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
import com.training.starter.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementServiceImpl implements StockMovementService {

    private static final int MAX_REFERENCE_ATTEMPTS = 5;
    private static final SecureRandom REFERENCE_RANDOM = new SecureRandom();

    private final StockMovementRepository stockMovementRepository;
    private final StockMovementItemRepository stockMovementItemRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final StockMovementMapper stockMovementMapper;

    @Override
    @Transactional
    public MovementResponse createImport(CreateImportRequest request) {
        log.debug("Creating import movement for warehouse id: {}", request.warehouseId());
        return createMovement(
                MovementType.IMPORT,
                request.warehouseId(),
                null,
                request.notes(),
                request.items());
    }

    @Override
    @Transactional
    public MovementResponse createExport(CreateExportRequest request) {
        log.debug("Creating export movement for warehouse id: {}", request.warehouseId());
        return createMovement(
                MovementType.EXPORT,
                request.warehouseId(),
                null,
                request.notes(),
                request.items());
    }

    @Override
    @Transactional
    public MovementResponse createTransfer(CreateTransferRequest request) {
        log.debug(
                "Creating transfer movement from warehouse id: {} to warehouse id: {}",
                request.warehouseId(),
                request.destWarehouseId());
        return createMovement(
                MovementType.TRANSFER,
                request.warehouseId(),
                request.destWarehouseId(),
                request.notes(),
                request.items());
    }

    @Override
    @Transactional(readOnly = true)
    public MovementResponse getById(Long id) {
        log.debug("Fetching stock movement by id: {}", id);
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockMovement", id));

        Warehouse warehouse = loadWarehouse(movement.getWarehouseId());
        Warehouse destWarehouse = movement.getDestWarehouseId() == null
                ? null
                : loadWarehouse(movement.getDestWarehouseId());

        List<StockMovementItem> items = stockMovementItemRepository.findByMovementId(id);
        Map<Long, Product> productsById = loadProductsByIds(items.stream()
                .map(StockMovementItem::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return stockMovementMapper.toResponse(
                movement, warehouse, destWarehouse, toItemResponses(items, productsById));
    }

    /**
     * Single transactional creation pipeline shared by import, export, and transfer. Validates
     * before mutating, persists the movement then its items, and never touches stock levels.
     */
    private MovementResponse createMovement(
            MovementType type,
            Long warehouseId,
            Long destWarehouseId,
            String notes,
            List<CreateMovementItemRequest> items) {

        validateItems(items);

        Warehouse warehouse = loadWarehouse(warehouseId);
        Warehouse destWarehouse = null;
        if (type == MovementType.TRANSFER) {
            if (destWarehouseId == null) {
                throw new BadRequestException("Transfer requires a destination warehouse");
            }
            if (destWarehouseId.equals(warehouseId)) {
                throw new BadRequestException(
                        "Transfer source and destination warehouses must be different");
            }
            destWarehouse = loadWarehouse(destWarehouseId);
        }

        Map<Long, Product> productsById = loadProductsByIds(items.stream()
                .map(CreateMovementItemRequest::productId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Long createdBy = resolveCurrentUserId();
        String referenceNo = generateReferenceNo(type);

        StockMovement movement = StockMovement.builder()
                .referenceNo(referenceNo)
                .type(type)
                .status(MovementStatus.PENDING_APPROVAL)
                .warehouseId(warehouseId)
                .destWarehouseId(type == MovementType.TRANSFER ? destWarehouseId : null)
                .notes(notes)
                .createdBy(createdBy)
                .build();
        StockMovement saved = stockMovementRepository.save(movement);

        List<StockMovementItem> itemEntities = items.stream()
                .map(item -> StockMovementItem.builder()
                        .movementId(saved.getId())
                        .productId(item.productId())
                        .quantity(item.quantity())
                        .unitCost(item.unitCost())
                        .batchNumber(item.batchNumber())
                        .expiryDate(item.expiryDate())
                        .notes(item.notes())
                        .build())
                .toList();
        List<StockMovementItem> savedItems = stockMovementItemRepository.saveAll(itemEntities);

        log.info(
                "Successfully created {} movement {} (id: {}) with {} item(s)",
                type,
                referenceNo,
                saved.getId(),
                savedItems.size());

        return stockMovementMapper.toResponse(
                saved, warehouse, destWarehouse, toItemResponses(savedItems, productsById));
    }

    /**
     * Guards enforced in the service rather than only on the DTO, because unit tests exercise the
     * service directly and bypass Bean Validation: at least one item, positive quantities, and no
     * duplicate product line within a single movement.
     */
    private void validateItems(List<CreateMovementItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one item is required");
        }
        Set<Long> productIds = new LinkedHashSet<>();
        for (CreateMovementItemRequest item : items) {
            if (item.productId() == null) {
                throw new BadRequestException("Product ID is required for every item");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Item quantity must be positive");
            }
            if (!productIds.add(item.productId())) {
                throw new BadRequestException(
                        "Duplicate product line for product id: " + item.productId());
            }
        }
    }

    private Warehouse loadWarehouse(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", warehouseId));
    }

    private Map<Long, Product> loadProductsByIds(Set<Long> productIds) {
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Long productId : productIds) {
            if (!productsById.containsKey(productId)) {
                throw new ResourceNotFoundException("Product", productId);
            }
        }
        return productsById;
    }

    private List<MovementItemResponse> toItemResponses(
            List<StockMovementItem> items, Map<Long, Product> productsById) {
        return items.stream()
                .map(item -> stockMovementMapper.toItemResponse(
                        item, productsById.get(item.getProductId())))
                .toList();
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException(
                    "No authenticated user is available to record as createdBy");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username))
                .getId();
    }

    private String generateReferenceNo(MovementType type) {
        String prefix = switch (type) {
            case IMPORT -> "IMP";
            case EXPORT -> "EXP";
            case TRANSFER -> "TRF";
            case ADJUSTMENT -> "ADJ";
        };
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = "%s-%s-%s".formatted(
                    prefix, datePart, String.format("%04X", REFERENCE_RANDOM.nextInt(0x10000)));
            if (!stockMovementRepository.existsByReferenceNo(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateResourceException("StockMovement", "referenceNo", prefix + "-" + datePart);
    }
}
