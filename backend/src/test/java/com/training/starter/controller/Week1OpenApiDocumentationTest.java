package com.training.starter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.starter.config.OpenApiConfig;
import com.training.starter.service.CategoryService;
import com.training.starter.service.ProductService;
import com.training.starter.service.WarehouseService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Week1OpenApiDocumentationTest.TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class Week1OpenApiDocumentationTest {

    private static final String BEARER_SCHEME = "Bearer Authentication";
    private static final Set<String> STOCKPULSE_PREFIXES = Set.of(
            "/api/v1/products", "/api/v1/categories", "/api/v1/warehouses");

    private static final Map<OperationKey, ExpectedOperation> EXPECTED_OPERATIONS = expectedOperations();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private WarehouseService warehouseService;

    private static JsonNode document;

    @BeforeAll
    static void resetDocument() {
        document = null;
    }

    @Test
    void generatedDocument_existingWeek1Operations_matchAllowlistAndMetadata() throws Exception {
        JsonNode api = document();

        assertThat(actualStockPulseOperations(api)).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS.keySet());
        assertThat(tagNames(api)).contains("Products", "Categories", "Warehouses");

        Set<String> operationIds = new HashSet<>();
        EXPECTED_OPERATIONS.forEach((key, expected) -> {
            JsonNode operation = operation(api, key);
            assertThat(operation.path("operationId").asText()).isEqualTo(expected.operationId());
            assertThat(operation.path("summary").asText()).isNotBlank();
            assertThat(operation.path("description").asText()).contains(expected.roleText());
            assertThat(textValues(operation.path("tags"))).containsExactly(expectedTag(key.path()));
            assertThat(operationIds.add(operation.path("operationId").asText())).isTrue();
            assertThat(responseCodes(operation)).containsExactlyInAnyOrderElementsOf(expected.responseCodes());
            assertBearerSecurity(operation);
            assertEnvelope(api, operation.path("responses").path(expected.successCode()));
            expected.responseCodes().stream()
                    .filter(code -> !code.equals(expected.successCode()))
                    .forEach(code -> assertEnvelope(api, operation.path("responses").path(code)));
        });

        JsonNode scheme = api.path("components").path("securitySchemes").path(BEARER_SCHEME);
        assertThat(scheme.path("type").asText()).isEqualTo("http");
        assertThat(scheme.path("scheme").asText()).isEqualTo("bearer");
        assertThat(scheme.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void generatedDocument_listAndSearchParameters_matchRuntimeContract() throws Exception {
        JsonNode api = document();

        for (OperationKey key : List.of(
                new OperationKey("/api/v1/products", "get"),
                new OperationKey("/api/v1/categories", "get"),
                new OperationKey("/api/v1/warehouses", "get"),
                new OperationKey("/api/v1/products/search", "get"))) {
            Map<String, JsonNode> parameters = parameters(operation(api, key));
            assertThat(parameters.keySet()).contains("page", "size", "sortBy", "sortDir");
            assertParameter(parameters.get("page"), "query", false, "0", "0");
            assertParameter(parameters.get("size"), "query", false, "20", "1");
            assertThat(parameters.get("sortBy").path("schema").path("default").asText()).isEqualTo("createdAt");
            assertThat(parameters.get("sortDir").path("schema").path("default").asText()).isEqualTo("DESC");
            assertThat(textValues(parameters.get("sortDir").path("schema").path("enum")))
                    .containsExactly("ASC", "DESC");
        }

        Map<String, JsonNode> searchParameters = parameters(
                operation(api, new OperationKey("/api/v1/products/search", "get")));
        assertThat(searchParameters.keySet()).containsExactlyInAnyOrder("q", "page", "size", "sortBy", "sortDir");
        assertThat(searchParameters.get("q").path("required").asBoolean()).isTrue();
        assertThat(searchParameters.get("q").path("schema").path("minLength").asInt()).isEqualTo(1);
        assertThat(textValues(searchParameters.get("sortBy").path("schema").path("enum")))
                .contains("id", "sku", "name", "categoryId", "createdAt", "updatedAt");
    }

    @Test
    void generatedDocument_requestSchemas_matchBeanValidationAndImmutableFields() throws Exception {
        JsonNode api = document();

        JsonNode createProduct = requestSchema(api, "/api/v1/products", "post");
        assertRequired(createProduct, "sku", "name", "categoryId", "unit", "minStock", "maxStock",
                "reorderPoint", "reorderQuantity");
        assertStringLimit(api, createProduct, "sku", 50);
        assertStringLimit(api, createProduct, "name", 255);
        assertStringLimit(api, createProduct, "unit", 20);
        assertMinimum(api, createProduct, "minStock", 0);
        assertMinimum(api, createProduct, "maxStock", 0);
        assertMinimum(api, createProduct, "reorderPoint", 0);
        assertMinimum(api, createProduct, "reorderQuantity", 1);

        JsonNode updateProduct = requestSchema(api, "/api/v1/products/{id}", "put");
        assertThat(propertyNames(api, updateProduct)).doesNotContain("sku");
        assertThat(requiredNames(api, updateProduct)).isEmpty();

        JsonNode createCategory = requestSchema(api, "/api/v1/categories", "post");
        assertRequired(createCategory, "name", "code");
        assertStringLimit(api, createCategory, "name", 100);
        assertStringLimit(api, createCategory, "code", 20);
        assertThat(propertyNames(api, requestSchema(api, "/api/v1/categories/{id}", "put")))
                .doesNotContain("code");

        JsonNode createWarehouse = requestSchema(api, "/api/v1/warehouses", "post");
        assertRequired(createWarehouse, "name", "code");
        assertStringLimit(api, createWarehouse, "name", 100);
        assertStringLimit(api, createWarehouse, "code", 20);
        assertThat(propertyNames(api, requestSchema(api, "/api/v1/warehouses/{id}", "put")))
                .doesNotContain("code");
    }

    @Test
    void generatedDocument_successSchemas_exposeConcreteEnvelopeAndPageModels() throws Exception {
        JsonNode api = document();

        assertPagedSuccess(api, "/api/v1/products", "get", Set.of(
                "id", "sku", "name", "categoryId", "unit", "minStock", "reorderPoint", "active", "createdAt"));
        assertPagedSuccess(api, "/api/v1/products/search", "get", Set.of(
                "id", "sku", "name", "categoryId", "unit", "minStock", "reorderPoint", "active", "createdAt"));
        assertPagedSuccess(api, "/api/v1/categories", "get", Set.of("id", "name", "code", "parentId", "createdAt"));
        assertPagedSuccess(api, "/api/v1/warehouses", "get", Set.of("id", "name", "code", "address", "active", "createdAt"));

        assertSingleSuccess(api, "/api/v1/products/{id}", "get", "200", Set.of(
                "id", "sku", "name", "description", "categoryId", "unit", "minStock", "maxStock",
                "reorderPoint", "reorderQuantity", "active", "createdAt"));
        assertSingleSuccess(api, "/api/v1/categories/{id}", "get", "200",
                Set.of("id", "name", "code", "parentId", "createdAt"));
        assertSingleSuccess(api, "/api/v1/warehouses/{id}", "get", "200",
                Set.of("id", "name", "code", "address", "active", "createdAt"));
        assertSingleSuccess(api, "/api/v1/products", "post", "201", Set.of(
                "id", "sku", "name", "description", "categoryId", "unit", "minStock", "maxStock",
                "reorderPoint", "reorderQuantity", "active", "createdAt"));
        assertSingleSuccess(api, "/api/v1/products/{id}", "put", "200", Set.of(
                "id", "sku", "name", "description", "categoryId", "unit", "minStock", "maxStock",
                "reorderPoint", "reorderQuantity", "active", "createdAt"));
        assertSingleSuccess(api, "/api/v1/categories", "post", "201",
                Set.of("id", "name", "code", "parentId", "createdAt"));
        assertSingleSuccess(api, "/api/v1/categories/{id}", "put", "200",
                Set.of("id", "name", "code", "parentId", "createdAt"));
        assertSingleSuccess(api, "/api/v1/warehouses", "post", "201",
                Set.of("id", "name", "code", "address", "active", "createdAt"));
        assertSingleSuccess(api, "/api/v1/warehouses/{id}", "put", "200",
                Set.of("id", "name", "code", "address", "active", "createdAt"));
    }

    @Test
    void swaggerUi_entryPointAndRenderedShell_areAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        String html = mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("swagger-ui", "swagger-initializer.js");
    }

    @Test
    void generatedDocument_unimplementedOperationsAndFuturePaths_areAbsent() throws Exception {
        JsonNode api = document();

        for (String path : List.of("/api/v1/products/{id}", "/api/v1/categories/{id}", "/api/v1/warehouses/{id}")) {
            assertThat(api.path("paths").path(path).has("delete")).isFalse();
        }

        List<String> forbiddenFragments = List.of(
                "/api/v1/stock", "/api/v1/movements", "/api/v1/alerts", "/api/v1/reorder-suggestions",
                "/dashboard", "/report", "/reports");
        pathNames(api).forEach(path -> forbiddenFragments.forEach(fragment ->
                assertThat(path).doesNotStartWith(fragment)));
    }

    private JsonNode document() throws Exception {
        if (document == null) {
            String json = mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            document = objectMapper.readTree(json);
        }
        return document;
    }

    private static void assertBearerSecurity(JsonNode operation) {
        assertThat(operation.path("security").isArray()).isTrue();
        assertThat(operation.path("security").findValues(BEARER_SCHEME)).isNotEmpty();
    }

    private static void assertParameter(JsonNode parameter, String location, boolean required,
                                        String defaultValue, String minimum) {
        assertThat(parameter.path("in").asText()).isEqualTo(location);
        assertThat(parameter.path("required").asBoolean()).isEqualTo(required);
        assertThat(parameter.path("schema").path("default").asText()).isEqualTo(defaultValue);
        assertThat(parameter.path("schema").path("minimum").asText()).isEqualTo(minimum);
    }

    private static void assertRequired(JsonNode schema, String... names) {
        assertThat(textValues(schema.path("required"))).contains(names);
    }

    private static void assertStringLimit(JsonNode api, JsonNode schema, String propertyName, int maximum) {
        assertThat(property(api, schema, propertyName).path("maxLength").asInt()).isEqualTo(maximum);
    }

    private static void assertMinimum(JsonNode api, JsonNode schema, String propertyName, int minimum) {
        assertThat(property(api, schema, propertyName).path("minimum").asInt()).isEqualTo(minimum);
    }

    private static void assertEnvelope(JsonNode api, JsonNode response) {
        JsonNode schema = response.path("content").path("application/json").path("schema");
        assertThat(propertyNames(api, schema)).contains("success", "message", "data", "timestamp");
    }

    private static void assertPagedSuccess(JsonNode api, String path, String method, Set<String> itemFields) {
        JsonNode response = operation(api, new OperationKey(path, method)).path("responses").path("200");
        JsonNode envelope = response.path("content").path("application/json").path("schema");
        assertEnvelope(api, response);
        JsonNode page = property(api, envelope, "data");
        assertThat(propertyNames(api, page)).containsExactlyInAnyOrder(
                "content", "page", "size", "totalElements", "totalPages", "last");
        JsonNode item = property(api, page, "content").path("items");
        assertThat(propertyNames(api, item)).containsAll(itemFields);
    }

    private static void assertSingleSuccess(JsonNode api, String path, String method, String status,
                                            Set<String> fields) {
        JsonNode response = operation(api, new OperationKey(path, method)).path("responses").path(status);
        JsonNode envelope = response.path("content").path("application/json").path("schema");
        assertEnvelope(api, response);
        assertThat(propertyNames(api, property(api, envelope, "data"))).containsAll(fields);
    }

    private static JsonNode requestSchema(JsonNode api, String path, String method) {
        JsonNode requestBody = operation(api, new OperationKey(path, method)).path("requestBody");
        assertThat(requestBody.path("required").asBoolean()).isTrue();
        return resolve(api, requestBody.path("content").path("application/json").path("schema"));
    }

    private static JsonNode operation(JsonNode api, OperationKey key) {
        JsonNode operation = api.path("paths").path(key.path()).path(key.method());
        assertThat(operation.isObject()).as("%s %s", key.method(), key.path()).isTrue();
        return operation;
    }

    private static Map<String, JsonNode> parameters(JsonNode operation) {
        Map<String, JsonNode> parameters = new LinkedHashMap<>();
        operation.path("parameters").forEach(parameter -> parameters.put(parameter.path("name").asText(), parameter));
        return parameters;
    }

    private static Set<String> responseCodes(JsonNode operation) {
        Set<String> codes = new HashSet<>();
        operation.path("responses").fieldNames().forEachRemaining(codes::add);
        return codes;
    }

    private static Set<OperationKey> actualStockPulseOperations(JsonNode api) {
        Set<OperationKey> operations = new HashSet<>();
        api.path("paths").fields().forEachRemaining(path -> {
            if (STOCKPULSE_PREFIXES.stream().anyMatch(path.getKey()::startsWith)) {
                path.getValue().fieldNames().forEachRemaining(method -> operations.add(new OperationKey(path.getKey(), method)));
            }
        });
        return operations;
    }

    private static Set<String> tagNames(JsonNode api) {
        Set<String> names = new HashSet<>();
        api.path("tags").forEach(tag -> names.add(tag.path("name").asText()));
        return names;
    }

    private static String expectedTag(String path) {
        if (path.startsWith("/api/v1/products")) {
            return "Products";
        }
        if (path.startsWith("/api/v1/categories")) {
            return "Categories";
        }
        return "Warehouses";
    }

    private static List<String> pathNames(JsonNode api) {
        List<String> paths = new ArrayList<>();
        api.path("paths").fieldNames().forEachRemaining(paths::add);
        return paths;
    }

    private static Set<String> propertyNames(JsonNode api, JsonNode schema) {
        Set<String> names = new HashSet<>();
        collectProperties(api, schema, names, null);
        return names;
    }

    private static Set<String> requiredNames(JsonNode api, JsonNode schema) {
        Set<String> names = new HashSet<>();
        collectRequired(api, schema, names);
        return names;
    }

    private static JsonNode property(JsonNode api, JsonNode schema, String name) {
        List<JsonNode> matches = new ArrayList<>();
        collectProperties(api, schema, null, new PropertySearch(name, matches));
        assertThat(matches).as("schema property %s", name).isNotEmpty();
        return resolve(api, matches.get(0));
    }

    private static void collectProperties(JsonNode api, JsonNode schema, Set<String> names, PropertySearch search) {
        JsonNode resolved = resolve(api, schema);
        Iterator<Map.Entry<String, JsonNode>> fields = resolved.path("properties").fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (names != null) {
                names.add(field.getKey());
            }
            if (search != null && search.name().equals(field.getKey())) {
                search.matches().add(field.getValue());
            }
        }
        resolved.path("allOf").forEach(part -> collectProperties(api, part, names, search));
    }

    private static void collectRequired(JsonNode api, JsonNode schema, Set<String> names) {
        JsonNode resolved = resolve(api, schema);
        names.addAll(textValues(resolved.path("required")));
        resolved.path("allOf").forEach(part -> collectRequired(api, part, names));
    }

    private static JsonNode resolve(JsonNode api, JsonNode schema) {
        JsonNode current = schema;
        Set<String> visited = new HashSet<>();
        while (current.has("$ref")) {
            String ref = current.path("$ref").asText();
            assertThat(visited.add(ref)).isTrue();
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            current = api.path("components").path("schemas").path(name);
            assertThat(current.isMissingNode()).as("resolved schema %s", ref).isFalse();
        }
        return current;
    }

    private static List<String> textValues(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private static Map<OperationKey, ExpectedOperation> expectedOperations() {
        Map<OperationKey, ExpectedOperation> operations = new LinkedHashMap<>();
        add(operations, "/api/v1/products", "get", "listProducts", "200", "STAFF, MANAGER, or ADMIN", "400", "401", "403");
        add(operations, "/api/v1/products/{id}", "get", "getProduct", "200", "STAFF, MANAGER, or ADMIN", "401", "403", "404");
        add(operations, "/api/v1/products/search", "get", "searchProducts", "200", "STAFF, MANAGER, or ADMIN", "400", "401", "403");
        add(operations, "/api/v1/products", "post", "createProduct", "201", "MANAGER or ADMIN", "400", "401", "403", "409");
        add(operations, "/api/v1/products/{id}", "put", "updateProduct", "200", "MANAGER or ADMIN", "400", "401", "403", "404");
        add(operations, "/api/v1/categories", "get", "listCategories", "200", "STAFF, MANAGER, or ADMIN", "400", "401", "403");
        add(operations, "/api/v1/categories/{id}", "get", "getCategory", "200", "STAFF, MANAGER, or ADMIN", "401", "403", "404");
        add(operations, "/api/v1/categories", "post", "createCategory", "201", "MANAGER or ADMIN", "400", "401", "403", "404", "409");
        add(operations, "/api/v1/categories/{id}", "put", "updateCategory", "200", "MANAGER or ADMIN", "400", "401", "403", "404");
        add(operations, "/api/v1/warehouses", "get", "listWarehouses", "200", "STAFF, MANAGER, or ADMIN", "400", "401", "403");
        add(operations, "/api/v1/warehouses/{id}", "get", "getWarehouse", "200", "STAFF, MANAGER, or ADMIN", "401", "403", "404");
        add(operations, "/api/v1/warehouses", "post", "createWarehouse", "201", "ADMIN only", "400", "401", "403", "409");
        add(operations, "/api/v1/warehouses/{id}", "put", "updateWarehouse", "200", "ADMIN only", "400", "401", "403", "404");
        return operations;
    }

    private static void add(Map<OperationKey, ExpectedOperation> operations, String path, String method,
                            String operationId, String successCode, String roleText, String... errors) {
        Set<String> responseCodes = new HashSet<>(List.of(errors));
        responseCodes.add(successCode);
        operations.put(new OperationKey(path, method),
                new ExpectedOperation(operationId, successCode, roleText, responseCodes));
    }

    private record OperationKey(String path, String method) {
    }

    private record ExpectedOperation(String operationId, String successCode, String roleText,
                                     Set<String> responseCodes) {
    }

    private record PropertySearch(String name, List<JsonNode> matches) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RabbitAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            MailSenderAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class
    })
    @Import({OpenApiConfig.class, ProductController.class, CategoryController.class, WarehouseController.class})
    public static class TestApplication {

        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }

        @Bean
        ProductService productService() {
            return org.mockito.Mockito.mock(ProductService.class);
        }

        @Bean
        CategoryService categoryService() {
            return org.mockito.Mockito.mock(CategoryService.class);
        }

        @Bean
        WarehouseService warehouseService() {
            return org.mockito.Mockito.mock(WarehouseService.class);
        }
    }
}
