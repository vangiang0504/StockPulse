package com.training.starter.repository;

import com.training.starter.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductSearchIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_search_test")
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

    private String marker;

    @BeforeEach
    void setUp() {
        marker = UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void searchByVector_matchesSkuAndNameCaseInsensitivelyAndHandlesPlainTextSpecialCharacters() {
        Product skuMatch = save("CASESKU-" + marker, "Ordinary item");
        Product nameMatch = save("OTHER-" + marker, "CaseName Gadget");
        Product specialMatch = save("SPECIAL-" + marker, "Gadget Pro Quoted");

        var bySku = productRepository.searchByVector(
                ("casesku " + marker).toUpperCase(), PageRequest.of(0, 10));
        var byName = productRepository.searchByVector("casename gadget", PageRequest.of(0, 10));
        var special = productRepository.searchByVector(
                "gadget & (pro) 'quoted'", PageRequest.of(0, 10));
        var noMatch = productRepository.searchByVector("missingterm" + marker, PageRequest.of(0, 10));

        assertThat(bySku.getContent()).extracting(Product::getId).containsExactly(skuMatch.getId());
        assertThat(byName.getContent()).extracting(Product::getId).containsExactly(nameMatch.getId());
        assertThat(special.getContent()).extracting(Product::getId).containsExactly(specialMatch.getId());
        assertThat(noMatch).isEmpty();
        assertThat(noMatch.getTotalElements()).isZero();
    }

    @Test
    void searchByVector_preservesDefaultSortingAndPaginationCountMetadata() {
        Product first = save("PAGE-1-" + marker, "PagedToken First");
        Product second = save("PAGE-2-" + marker, "PagedToken Second");
        Product third = save("PAGE-3-" + marker, "PagedToken Third");
        Product fourth = save("PAGE-4-" + marker, "PagedToken Fourth");
        Product fifth = save("PAGE-5-" + marker, "PagedToken Fifth");

        var sort = Sort.by(Sort.Direction.DESC, "created_at");
        var page0 = productRepository.searchByVector("pagedtoken", PageRequest.of(0, 2, sort));
        var page1 = productRepository.searchByVector("pagedtoken", PageRequest.of(1, 2, sort));
        var page2 = productRepository.searchByVector("pagedtoken", PageRequest.of(2, 2, sort));

        assertThat(page0.getContent()).extracting(Product::getId)
                .containsExactly(fifth.getId(), fourth.getId());
        assertThat(page1.getContent()).extracting(Product::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(page2.getContent()).extracting(Product::getId)
                .containsExactly(first.getId());
        assertThat(page0.getNumber()).isZero();
        assertThat(page0.getSize()).isEqualTo(2);
        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.isLast()).isFalse();
        assertThat(page2.isLast()).isTrue();
    }

    private Product save(String sku, String name) {
        Product product = Product.builder()
                .sku(sku)
                .name(name)
                .unit("PCS")
                .minStock(10)
                .maxStock(100)
                .reorderPoint(20)
                .reorderQuantity(50)
                .active(true)
                .build();
        return productRepository.saveAndFlush(product);
    }
}
