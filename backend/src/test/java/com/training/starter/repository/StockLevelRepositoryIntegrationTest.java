package com.training.starter.repository;

import com.training.starter.entity.Product;
import com.training.starter.entity.StockLevel;
import com.training.starter.entity.Warehouse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StockLevelRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("stock_level_repository_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private StockSummaryRepository stockSummaryRepository;

    @Autowired
    private EntityManager entityManager;

    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        product = productRepository.saveAndFlush(Product.builder()
                .sku("STK-" + marker)
                .name("Stock lookup product")
                .unit("PCS")
                .minStock(10)
                .maxStock(100)
                .reorderPoint(20)
                .reorderQuantity(50)
                .active(true)
                .build());
        warehouse = warehouseRepository.saveAndFlush(Warehouse.builder()
                .name("Stock lookup warehouse")
                .code("SL" + marker)
                .address("Repository integration test")
                .active(true)
                .build());
    }

    @Test
    void findByWarehouseIdAndProductId_existingPair_returnsMappedStockLevel() {
        // Given
        StockLevel saved = stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantity(40)
                .reservedQuantity(7)
                .build());

        // When
        var result = stockLevelRepository.findByWarehouseIdAndProductId(
                warehouse.getId(), product.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(saved.getId());
        assertThat(result.orElseThrow().getProductId()).isEqualTo(product.getId());
        assertThat(result.orElseThrow().getWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(result.orElseThrow().getQuantity()).isEqualTo(40);
        assertThat(result.orElseThrow().getReservedQuantity()).isEqualTo(7);
        assertThat(result.orElseThrow().getVersion()).isZero();
        assertThat(result.orElseThrow().getUpdatedAt()).isNotNull();
    }

    @Test
    void findByWarehouseIdAndProductId_missingPair_returnsEmpty() {
        // Given
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantity(10)
                .reservedQuantity(0)
                .build());

        // When
        var result = stockLevelRepository.findByWarehouseIdAndProductId(
                warehouse.getId(), Long.MAX_VALUE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllWithDisplayData_filtersProvided_returnsEnrichedMatchingRow() {
        // Given
        Product secondProduct = saveProduct("Filtered product", 35);
        Warehouse secondWarehouse = saveWarehouse("Filtered warehouse");
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantity(40)
                .reservedQuantity(7)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(secondProduct.getId())
                .warehouseId(warehouse.getId())
                .quantity(30)
                .reservedQuantity(4)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(secondProduct.getId())
                .warehouseId(secondWarehouse.getId())
                .quantity(20)
                .reservedQuantity(2)
                .build());

        // When
        var result = stockLevelRepository.findAllWithDisplayData(
                warehouse.getId(),
                secondProduct.getId(),
                PageRequest.of(0, 20));

        // Then
        assertThat(result.getContent()).hasSize(1);
        var row = result.getContent().get(0);
        assertThat(row.getProductId()).isEqualTo(secondProduct.getId());
        assertThat(row.getProductSku()).isEqualTo(secondProduct.getSku());
        assertThat(row.getProductName()).isEqualTo("Filtered product");
        assertThat(row.getWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(row.getWarehouseCode()).isEqualTo(warehouse.getCode());
        assertThat(row.getWarehouseName()).isEqualTo("Stock lookup warehouse");
        assertThat(row.getQuantity()).isEqualTo(30);
        assertThat(row.getReservedQuantity()).isEqualTo(4);
        assertThat(row.getAvailableQuantity()).isEqualTo(26);
        assertThat(row.getReorderPoint()).isEqualTo(35);
    }

    @Test
    void findAllWithDisplayData_filtersOmitted_returnsZeroBasedPageMetadata() {
        // Given
        Product secondProduct = saveProduct("Paged product", 25);
        Warehouse secondWarehouse = saveWarehouse("Paged warehouse");
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantity(10)
                .reservedQuantity(1)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(secondProduct.getId())
                .warehouseId(warehouse.getId())
                .quantity(20)
                .reservedQuantity(2)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(product.getId())
                .warehouseId(secondWarehouse.getId())
                .quantity(30)
                .reservedQuantity(3)
                .build());

        // When
        var firstPage = stockLevelRepository.findAllWithDisplayData(
                null,
                null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id")));
        var secondPage = stockLevelRepository.findAllWithDisplayData(
                null,
                null,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "id")));

        // Then
        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isLast()).isFalse();
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    void findLowStockWithDisplayData_usesProductReorderPointAndOptionalWarehouseFilter() {
        // Given
        Product belowFixedThresholdButNotLow = saveProduct("Low reorder point", 5);
        Product aboveFixedThresholdButLow = saveProduct("High reorder point", 30);
        Warehouse secondWarehouse = saveWarehouse("Second low-stock warehouse");
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(belowFixedThresholdButNotLow.getId())
                .warehouseId(warehouse.getId())
                .quantity(10)
                .reservedQuantity(0)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(aboveFixedThresholdButLow.getId())
                .warehouseId(warehouse.getId())
                .quantity(30)
                .reservedQuantity(4)
                .build());
        stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(aboveFixedThresholdButLow.getId())
                .warehouseId(secondWarehouse.getId())
                .quantity(25)
                .reservedQuantity(5)
                .build());

        // When
        var warehouseResult = stockLevelRepository.findLowStockWithDisplayData(
                warehouse.getId(),
                PageRequest.of(0, 20));
        var allWarehousesResult = stockLevelRepository.findLowStockWithDisplayData(
                null,
                PageRequest.of(0, 20));

        // Then
        assertThat(warehouseResult.getContent()).hasSize(1);
        var row = warehouseResult.getContent().get(0);
        assertThat(row.getProductId()).isEqualTo(aboveFixedThresholdButLow.getId());
        assertThat(row.getWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(row.getQuantity()).isEqualTo(30);
        assertThat(row.getAvailableQuantity()).isEqualTo(26);
        assertThat(row.getReorderPoint()).isEqualTo(30);
        assertThat(allWarehousesResult.getContent())
                .extracting(projection -> projection.getWarehouseId())
                .containsExactlyInAnyOrder(warehouse.getId(), secondWarehouse.getId());
    }

    @Test
    void findAllWithFilters_refreshedView_mapsAllStatusesFieldsAndPageMetadata() {
        // Given
        Product outOfStock = saveSummaryProduct("Out of stock", 10, 100, 20);
        Product lowStock = saveSummaryProduct("Low stock", 10, 100, 20);
        Product overstock = saveSummaryProduct("Overstock", 10, 100, 20);
        Product normal = saveSummaryProduct("Normal stock", 10, 100, 20);
        saveStockLevel(outOfStock, warehouse, 0, 0);
        saveStockLevel(lowStock, warehouse, 10, 2);
        saveStockLevel(overstock, warehouse, 100, 5);
        saveStockLevel(normal, warehouse, 50, 7);
        refreshStockSummary();

        // When
        var firstPage = stockSummaryRepository.findAllWithFilters(
                warehouse.getId(),
                null,
                PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "product_id")));
        var secondPage = stockSummaryRepository.findAllWithFilters(
                warehouse.getId(),
                null,
                PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "product_id")));

        // Then
        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent())
                .extracting(projection -> projection.getStockStatus())
                .containsAnyOf("OUT_OF_STOCK", "LOW_STOCK", "OVERSTOCK", "NORMAL");

        var allRows = stockSummaryRepository.findAllWithFilters(
                warehouse.getId(),
                null,
                PageRequest.of(0, 20));
        assertThat(allRows.getContent())
                .extracting(projection -> projection.getStockStatus())
                .containsExactlyInAnyOrder(
                        "OUT_OF_STOCK", "LOW_STOCK", "OVERSTOCK", "NORMAL");

        var normalRow = allRows.getContent().stream()
                .filter(row -> row.getProductId().equals(normal.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(normalRow.getSku()).isEqualTo(normal.getSku());
        assertThat(normalRow.getProductName()).isEqualTo("Normal stock");
        assertThat(normalRow.getCategoryName()).isNull();
        assertThat(normalRow.getWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(normalRow.getWarehouseName()).isEqualTo(warehouse.getName());
        assertThat(normalRow.getQuantity()).isEqualTo(50);
        assertThat(normalRow.getReservedQuantity()).isEqualTo(7);
        assertThat(normalRow.getAvailableQuantity()).isEqualTo(43);
        assertThat(normalRow.getMinStock()).isEqualTo(10);
        assertThat(normalRow.getReorderPoint()).isEqualTo(20);
        assertThat(normalRow.getStockStatus()).isEqualTo("NORMAL");
    }

    @Test
    void findAllWithFilters_productAndWarehouseFilters_returnExactSummaryRow() {
        // Given
        Product matchingProduct = saveSummaryProduct("Matching summary", 5, 50, 10);
        Product otherProduct = saveSummaryProduct("Other summary", 5, 50, 10);
        Warehouse otherWarehouse = saveWarehouse("Other summary warehouse");
        saveStockLevel(matchingProduct, warehouse, 15, 3);
        saveStockLevel(otherProduct, warehouse, 20, 4);
        saveStockLevel(matchingProduct, otherWarehouse, 25, 5);
        refreshStockSummary();

        // When
        var result = stockSummaryRepository.findAllWithFilters(
                warehouse.getId(),
                matchingProduct.getId(),
                PageRequest.of(0, 20));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductId())
                .isEqualTo(matchingProduct.getId());
        assertThat(result.getContent().get(0).getWarehouseId())
                .isEqualTo(warehouse.getId());
    }

    @Test
    void summaryAfterBaseStockUpdate_remainsAtLastRefreshWhileTransactionalListIsCurrent() {
        // Given
        Product snapshotProduct = saveSummaryProduct("Snapshot product", 10, 100, 20);
        StockLevel stockLevel = saveStockLevel(snapshotProduct, warehouse, 40, 5);
        refreshStockSummary();

        // When
        stockLevel.setQuantity(8);
        stockLevelRepository.saveAndFlush(stockLevel);
        entityManager.clear();

        var transactionalResult = stockLevelRepository.findAllWithDisplayData(
                warehouse.getId(),
                snapshotProduct.getId(),
                PageRequest.of(0, 20));
        var summaryResult = stockSummaryRepository.findAllWithFilters(
                warehouse.getId(),
                snapshotProduct.getId(),
                PageRequest.of(0, 20));

        // Then
        assertThat(transactionalResult.getContent()).singleElement()
                .satisfies(row -> assertThat(row.getQuantity()).isEqualTo(8));
        assertThat(summaryResult.getContent()).singleElement()
                .satisfies(row -> {
                    assertThat(row.getQuantity()).isEqualTo(40);
                    assertThat(row.getStockStatus()).isEqualTo("NORMAL");
                });
    }

    private Product saveProduct(String name, int reorderPoint) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return productRepository.saveAndFlush(Product.builder()
                .sku("SP-" + marker)
                .name(name)
                .unit("PCS")
                .minStock(10)
                .maxStock(100)
                .reorderPoint(reorderPoint)
                .reorderQuantity(50)
                .active(true)
                .build());
    }

    private Product saveSummaryProduct(
            String name,
            int minStock,
            int maxStock,
            int reorderPoint) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return productRepository.saveAndFlush(Product.builder()
                .sku("SS-" + marker)
                .name(name)
                .unit("PCS")
                .minStock(minStock)
                .maxStock(maxStock)
                .reorderPoint(reorderPoint)
                .reorderQuantity(50)
                .active(true)
                .build());
    }

    private StockLevel saveStockLevel(
            Product stockProduct,
            Warehouse stockWarehouse,
            int quantity,
            int reservedQuantity) {
        return stockLevelRepository.saveAndFlush(StockLevel.builder()
                .productId(stockProduct.getId())
                .warehouseId(stockWarehouse.getId())
                .quantity(quantity)
                .reservedQuantity(reservedQuantity)
                .build());
    }

    private void refreshStockSummary() {
        entityManager.flush();
        entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW mv_stock_summary")
                .executeUpdate();
    }

    private Warehouse saveWarehouse(String name) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return warehouseRepository.saveAndFlush(Warehouse.builder()
                .name(name)
                .code("SW" + marker)
                .address("Repository integration test")
                .active(true)
                .build());
    }
}
