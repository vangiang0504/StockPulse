package com.training.starter.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.starter.BaseIntegrationTest;
import com.training.starter.entity.Category;
import com.training.starter.entity.Product;
import com.training.starter.entity.User;
import com.training.starter.entity.Warehouse;
import com.training.starter.enums.Role;
import com.training.starter.repository.CategoryRepository;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.UserRepository;
import com.training.starter.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogWarehouseAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<Role, String> tokens = new EnumMap<>(Role.class);

    private Category category;
    private Product product;
    private Warehouse warehouse;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        for (Role role : Role.values()) {
            String username = "auth-" + role.name().toLowerCase();
            User user = userRepository.findByUsername(username).orElseGet(() -> User.builder()
                    .username(username)
                    .email(username + "@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .role(role)
                    .active(true)
                    .build());
            user.setRole(role);
            user.setActive(true);
            userRepository.save(user);
            tokens.put(role, jwtTokenProvider.generateAccessToken(username));
        }

        category = categoryRepository.save(Category.builder()
                .name("Authorization category " + suffix)
                .code("AC" + suffix)
                .build());
        product = productRepository.save(Product.builder()
                .sku("AS" + suffix)
                .name("Authorization product " + suffix)
                .categoryId(category.getId())
                .unit("PCS")
                .minStock(1)
                .maxStock(100)
                .reorderPoint(5)
                .reorderQuantity(10)
                .active(true)
                .build());
        warehouse = warehouseRepository.save(Warehouse.builder()
                .name("Authorization warehouse " + suffix)
                .code("AW" + suffix)
                .address("Test address")
                .active(true)
                .build());
    }

    @Test
    void unauthenticatedCatalogAndWarehouseRequestsReturnStandard401Envelope() throws Exception {
        String[] reads = {
                "/api/v1/products",
                "/api/v1/categories",
                "/api/v1/warehouses"
        };

        for (String path : reads) {
            assertError(exchange(path, HttpMethod.GET, null, null), HttpStatus.UNAUTHORIZED);
        }

        assertError(exchange("/api/v1/products", HttpMethod.POST, productCreateBody("UP"), null),
                HttpStatus.UNAUTHORIZED);
        assertError(exchange("/api/v1/categories", HttpMethod.POST, categoryCreateBody("UC"), null),
                HttpStatus.UNAUTHORIZED);
        assertError(exchange("/api/v1/warehouses", HttpMethod.POST, warehouseCreateBody("UW"), null),
                HttpStatus.UNAUTHORIZED);
    }

    @Test
    void everyRoleFollowsTheReadAuthorizationMatrix() throws Exception {
        for (Role role : Role.values()) {
            HttpStatus expected = role == Role.USER ? HttpStatus.FORBIDDEN : HttpStatus.OK;
            String[] reads = {
                    "/api/v1/products",
                    "/api/v1/products/" + product.getId(),
                    "/api/v1/products/search?q=Authorization",
                    "/api/v1/categories",
                    "/api/v1/categories/" + category.getId(),
                    "/api/v1/warehouses",
                    "/api/v1/warehouses/" + warehouse.getId()
            };

            for (String path : reads) {
                ResponseEntity<String> response = exchange(path, HttpMethod.GET, null, role);
                assertThat(response.getStatusCode()).as("%s reading %s", role, path).isEqualTo(expected);
                if (expected == HttpStatus.FORBIDDEN) {
                    assertError(response, expected);
                }
            }
        }
    }

    @Test
    void everyRoleFollowsTheCreateAuthorizationMatrixWithoutDeniedMutations() throws Exception {
        for (Role role : Role.values()) {
            assertCreate(role, "products", productCreateBody("P" + role.ordinal()),
                    role == Role.MANAGER || role == Role.ADMIN);
            assertCreate(role, "categories", categoryCreateBody("C" + role.ordinal()),
                    role == Role.MANAGER || role == Role.ADMIN);
            assertCreate(role, "warehouses", warehouseCreateBody("W" + role.ordinal()),
                    role == Role.ADMIN);
        }
    }

    @Test
    void everyRoleFollowsTheUpdateAuthorizationMatrixWithoutDeniedMutations() throws Exception {
        for (Role role : Role.values()) {
            assertUpdate(role, "products", product.getId(), Map.of("name", "Product by " + role),
                    role == Role.MANAGER || role == Role.ADMIN);
            assertUpdate(role, "categories", category.getId(), Map.of("name", "Category by " + role),
                    role == Role.MANAGER || role == Role.ADMIN);
            assertUpdate(role, "warehouses", warehouse.getId(), Map.of("name", "Warehouse by " + role),
                    role == Role.ADMIN);
        }
    }

    private void assertCreate(Role role, String resource, Map<String, Object> body, boolean allowed)
            throws Exception {
        long before = repositoryCount(resource);
        ResponseEntity<String> response = exchange("/api/v1/" + resource, HttpMethod.POST, body, role);

        if (allowed) {
            assertThat(response.getStatusCode()).as("%s creating %s", role, resource)
                    .isEqualTo(HttpStatus.CREATED);
            assertThat(repositoryCount(resource)).isEqualTo(before + 1);
        } else {
            assertError(response, HttpStatus.FORBIDDEN);
            assertThat(repositoryCount(resource)).as("Denied %s create must not mutate state", resource)
                    .isEqualTo(before);
        }
    }

    private void assertUpdate(Role role, String resource, Long id, Map<String, Object> body, boolean allowed)
            throws Exception {
        String before = entityName(resource, id);
        ResponseEntity<String> response = exchange(
                "/api/v1/" + resource + "/" + id, HttpMethod.PUT, body, role);

        if (allowed) {
            assertThat(response.getStatusCode()).as("%s updating %s", role, resource)
                    .isEqualTo(HttpStatus.OK);
            assertThat(entityName(resource, id)).isEqualTo(body.get("name"));
        } else {
            assertError(response, HttpStatus.FORBIDDEN);
            assertThat(entityName(resource, id)).as("Denied %s update must not mutate state", resource)
                    .isEqualTo(before);
        }
    }

    private ResponseEntity<String> exchange(
            String path, HttpMethod method, Object body, Role role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (role != null) {
            headers.setBearerAuth(tokens.get(role));
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private void assertError(ResponseEntity<String> response, HttpStatus status) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(status);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("message").asText()).isNotBlank();
        assertThat(json.path("timestamp").asText()).isNotBlank();
    }

    private Map<String, Object> productCreateBody(String marker) {
        return Map.of(
                "sku", "P" + marker + suffix,
                "name", "Created product " + marker,
                "description", "Authorization test",
                "categoryId", category.getId(),
                "unit", "PCS",
                "minStock", 1,
                "maxStock", 100,
                "reorderPoint", 5,
                "reorderQuantity", 10);
    }

    private Map<String, Object> categoryCreateBody(String marker) {
        return Map.of(
                "name", "Created category " + marker,
                "code", "C" + marker + suffix);
    }

    private Map<String, Object> warehouseCreateBody(String marker) {
        return Map.of(
                "name", "Created warehouse " + marker,
                "code", "W" + marker + suffix,
                "address", "Authorization test");
    }

    private long repositoryCount(String resource) {
        return switch (resource) {
            case "products" -> productRepository.count();
            case "categories" -> categoryRepository.count();
            case "warehouses" -> warehouseRepository.count();
            default -> throw new IllegalArgumentException("Unknown resource: " + resource);
        };
    }

    private String entityName(String resource, Long id) {
        return switch (resource) {
            case "products" -> productRepository.findById(id).orElseThrow().getName();
            case "categories" -> categoryRepository.findById(id).orElseThrow().getName();
            case "warehouses" -> warehouseRepository.findById(id).orElseThrow().getName();
            default -> throw new IllegalArgumentException("Unknown resource: " + resource);
        };
    }
}
