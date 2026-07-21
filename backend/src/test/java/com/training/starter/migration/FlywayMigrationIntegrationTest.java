package com.training.starter.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayMigrationIntegrationTest {

    private static final String MIGRATION_LOCATION = "classpath:db/migration";
    private static final String V1_SHA256 = "45524b330d1fe645ccd7b3d130bfc19505af2edddeeeb5d2c1b131ecb625e147";
    private static final String V2_SHA256 = "7fa2600b6868d2cebbb6772f292a6bc359918549c10723755ef5bd1d83ddabe8";

    private static final String EXTERNAL_JDBC_URL = System.getProperty("migration.test.jdbc-url");
    private static final String EXTERNAL_USERNAME = System.getProperty("migration.test.username", "postgres");
    private static final String EXTERNAL_PASSWORD = System.getProperty("migration.test.password", "postgres");
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("migration_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startPostgres() {
        if (EXTERNAL_JDBC_URL == null) {
            POSTGRES.start();
        }
    }

    @AfterAll
    static void stopPostgres() {
        if (EXTERNAL_JDBC_URL == null) {
            POSTGRES.stop();
        }
    }

    @Test
    void cleanMigrationAndRestartValidationSucceedWithoutChangingReleasedMigrations() throws Exception {
        assertThat(sha256("db/migration/V1__create_users_table.sql")).isEqualTo(V1_SHA256);
        assertThat(sha256("db/migration/V2__create_warehouse_tables.sql")).isEqualTo(V2_SHA256);

        String schema = newSchema();
        MigrateResult result = flyway(schema).migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(8);
        assertThat(queryInt(schema,
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL"))
                .isEqualTo(7);
        assertThat(queryInt(schema,
                "SELECT count(*) FROM flyway_schema_history WHERE success AND type = 'SQL' AND version IS NULL"))
                .isEqualTo(1);

        ValidateResult validation = flyway(schema).validateWithResult();
        assertThat(validation.validationSuccessful).isTrue();
        assertThat(validation.invalidMigrations).isEmpty();
    }

    @Test
    void populatedV2DatabaseUpgradesToV7WithoutDataLossAndBackfillsSearchVector() throws Exception {
        String schema = newSchema();
        flyway(schema, MigrationVersion.fromVersion("2")).migrate();

        execute(schema, """
                INSERT INTO users (id, username, email, password, full_name, role)
                VALUES (100, 'sentinel-user', 'sentinel@example.com', 'hash', 'Sentinel User', 'USER');
                INSERT INTO categories (id, name, code) VALUES (100, 'Sentinel Category', 'SENT-CAT');
                INSERT INTO warehouses (id, name, code, address) VALUES (100, 'Sentinel Warehouse', 'SENT-WH', 'Keep me');
                INSERT INTO products (id, sku, name, description, category_id, search_vector)
                VALUES (100, 'Sent-100', 'Upgrade Product', 'Keep me too', 100, NULL);
                """);

        MigrateResult result = flyway(schema).migrate();

        assertThat(result.success).isTrue();
        assertThat(queryString(schema, "SELECT full_name FROM users WHERE id = 100")).isEqualTo("Sentinel User");
        assertThat(queryString(schema, "SELECT address FROM warehouses WHERE id = 100")).isEqualTo("Keep me");
        assertThat(queryString(schema, "SELECT description FROM products WHERE id = 100")).isEqualTo("Keep me too");
        assertThat(queryInt(schema, "SELECT count(*) FROM categories WHERE id BETWEEN 1 AND 10")).isEqualTo(10);
        assertThat(queryBoolean(schema, """
                SELECT search_vector @@ plainto_tsquery('simple', 'sent')
                       AND search_vector @@ plainto_tsquery('simple', 'upgrade')
                FROM products WHERE id = 100
                """)).isTrue();
        assertThat(queryInt(schema,
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version BETWEEN '3' AND '7'"))
                .isEqualTo(5);
    }

    @Test
    void schemaMetadataContainsRequiredObjectsConstraintsIndexesAndTrigger() throws Exception {
        String schema = migratedSchema();

        assertThat(columns(schema, "stock_levels")).containsExactly(
                "id", "product_id", "warehouse_id", "quantity", "reserved_quantity", "version", "updated_at");
        assertThat(columns(schema, "stock_movements")).containsExactly(
                "id", "reference_no", "type", "status", "warehouse_id", "dest_warehouse_id", "notes",
                "created_by", "approved_by", "created_at", "updated_at");
        assertThat(columns(schema, "stock_movement_items")).containsExactly(
                "id", "movement_id", "product_id", "quantity", "unit_cost", "batch_number", "expiry_date", "notes");
        assertThat(columns(schema, "stock_alerts")).containsExactly(
                "id", "product_id", "warehouse_id", "alert_type", "current_quantity", "threshold", "status",
                "created_at", "resolved_at");
        assertThat(columns(schema, "reorder_suggestions")).containsExactly(
                "id", "product_id", "warehouse_id", "suggested_quantity", "current_stock", "reorder_point",
                "status", "created_at", "updated_at");

        assertThat(names(schema, """
                SELECT conname FROM pg_constraint c
                JOIN pg_namespace n ON n.oid = c.connamespace
                WHERE n.nspname = '%s' AND c.contype = 'c'
                """.formatted(schema))).contains(
                "chk_stock_quantity_non_negative", "chk_stock_reserved_quantity_non_negative",
                "chk_stock_version_non_negative", "chk_movement_type", "chk_movement_status",
                "chk_movement_destination", "chk_movement_item_quantity_positive",
                "chk_movement_item_unit_cost_non_negative", "chk_stock_alert_type", "chk_stock_alert_status",
                "chk_stock_alert_current_quantity_non_negative", "chk_stock_alert_threshold_non_negative",
                "chk_stock_alert_resolution", "chk_reorder_suggested_quantity_positive",
                "chk_reorder_current_stock_non_negative", "chk_reorder_point_non_negative", "chk_reorder_status");

        assertThat(names(schema, "SELECT indexname FROM pg_indexes WHERE schemaname = '%s'".formatted(schema)))
                .contains("idx_products_sku", "idx_products_category", "idx_products_search", "idx_stock_product",
                        "idx_stock_warehouse", "idx_stock_low", "idx_movements_warehouse",
                        "idx_movements_type_status", "idx_movements_created", "idx_movement_items_product",
                        "idx_alerts_active", "uk_stock_alert_unresolved", "uk_reorder_suggestion_pending",
                        "idx_mv_stock_summary");
        assertThat(queryInt(schema, """
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'products'
                  AND column_name = 'search_vector' AND is_nullable = 'NO'
                """)).isEqualTo(1);
        assertThat(queryInt(schema, """
                SELECT count(*) FROM pg_trigger t
                JOIN pg_class c ON c.oid = t.tgrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = current_schema() AND t.tgname = 'trg_products_search_vector_update'
                  AND NOT t.tgisinternal
                """)).isEqualTo(1);
        assertThat(queryInt(schema,
                "SELECT count(*) FROM pg_matviews WHERE schemaname = current_schema() AND matviewname = 'mv_stock_summary'"))
                .isEqualTo(1);
        assertThat(queryInt(schema, """
                SELECT count(*) FROM information_schema.table_constraints
                WHERE constraint_schema = current_schema() AND constraint_type = 'FOREIGN KEY'
                  AND table_name IN ('stock_levels', 'stock_movements', 'stock_movement_items', 'stock_alerts', 'reorder_suggestions')
                """)).isEqualTo(12);
    }

    @Test
    void databaseRejectsInvalidDomainValuesAndEnforcesConditionalUniqueness() throws Exception {
        String schema = migratedSchema();
        insertParents(schema);

        execute(schema, "INSERT INTO stock_levels (product_id, warehouse_id) VALUES (100, 100)");
        assertRejected(schema, "INSERT INTO stock_levels (product_id, warehouse_id) VALUES (100, 100)");
        assertRejected(schema, "INSERT INTO stock_levels (product_id, warehouse_id, quantity) VALUES (100, 101, -1)");
        assertRejected(schema, "INSERT INTO stock_levels (product_id, warehouse_id, reserved_quantity) VALUES (100, 101, -1)");
        assertRejected(schema, "INSERT INTO stock_levels (product_id, warehouse_id, version) VALUES (100, 101, -1)");
        assertRejected(schema, "INSERT INTO stock_levels (product_id, warehouse_id) VALUES (99999, 101)");

        assertRejected(schema, movement("BAD", "DRAFT", "NULL"));
        assertRejected(schema, movement("IMPORT", "BAD", "NULL"));
        assertRejected(schema, movement("IMPORT", "DRAFT", "101"));
        assertRejected(schema, movement("TRANSFER", "DRAFT", "NULL"));
        assertRejected(schema, movement("TRANSFER", "DRAFT", "100"));
        execute(schema, """
                INSERT INTO stock_movements (id, reference_no, type, warehouse_id, dest_warehouse_id, created_by)
                VALUES (100, 'MOV-VALID', 'TRANSFER', 100, 101, 100)
                """);
        assertRejected(schema, """
                INSERT INTO stock_movements (reference_no, type, warehouse_id, created_by)
                VALUES ('MOV-VALID', 'IMPORT', 100, 100)
                """);
        assertRejected(schema, "INSERT INTO stock_movement_items (movement_id, product_id, quantity) VALUES (100, 100, 0)");
        assertRejected(schema, "INSERT INTO stock_movement_items (movement_id, product_id, quantity) VALUES (100, 100, -1)");
        assertRejected(schema, "INSERT INTO stock_movement_items (movement_id, product_id, quantity, unit_cost) VALUES (100, 100, 1, -0.01)");
        assertRejected(schema, "INSERT INTO stock_movement_items (movement_id, product_id, quantity) VALUES (99999, 100, 1)");

        assertRejected(schema, alert("BAD", 1, 1, "ACTIVE", "NULL"));
        assertRejected(schema, alert("LOW_STOCK", -1, 1, "ACTIVE", "NULL"));
        assertRejected(schema, alert("LOW_STOCK", 1, -1, "ACTIVE", "NULL"));
        assertRejected(schema, alert("LOW_STOCK", 1, 1, "RESOLVED", "NULL"));
        execute(schema, alert("LOW_STOCK", 1, 1, "ACTIVE", "NULL"));
        assertRejected(schema, alert("LOW_STOCK", 2, 1, "ACKNOWLEDGED", "NULL"));
        execute(schema, "UPDATE stock_alerts SET status = 'RESOLVED', resolved_at = NOW()");
        execute(schema, alert("LOW_STOCK", 2, 1, "ACTIVE", "NULL"));

        assertRejected(schema, reorder(0, 1, 1, "PENDING"));
        assertRejected(schema, reorder(1, -1, 1, "PENDING"));
        assertRejected(schema, reorder(1, 1, -1, "PENDING"));
        assertRejected(schema, reorder(1, 1, 1, "BAD"));
        execute(schema, reorder(1, 1, 1, "PENDING"));
        assertRejected(schema, reorder(2, 1, 1, "PENDING"));
        execute(schema, "UPDATE reorder_suggestions SET status = 'REJECTED'");
        execute(schema, reorder(2, 1, 1, "PENDING"));
    }

    @Test
    void productSearchVectorIsMaintainedForInsertAndSkuOrNameUpdates() throws Exception {
        String schema = migratedSchema();
        execute(schema, """
                INSERT INTO products (id, sku, name) VALUES (100, 'MiXeD-SKU-42', 'Portable Scanner');
                """);

        assertThat(matches(schema, 100, "mixed")).isTrue();
        assertThat(matches(schema, 100, "portable")).isTrue();
        assertThat(matches(schema, 100, "unrelated")).isFalse();

        execute(schema, "UPDATE products SET name = 'Desktop Reader' WHERE id = 100");
        assertThat(matches(schema, 100, "portable")).isFalse();
        assertThat(matches(schema, 100, "desktop")).isTrue();
    }

    @Test
    void materializedViewProjectsStatusesAndSupportsConcurrentRefresh() throws Exception {
        String schema = migratedSchema();
        execute(schema, """
                INSERT INTO warehouses (id, name, code) VALUES (100, 'Primary', 'WH-100');
                INSERT INTO products (id, sku, name, min_stock, max_stock, reorder_point, active) VALUES
                    (100, 'ZERO', 'Zero Product', 10, 100, 20, TRUE),
                    (101, 'LOW', 'Low Product', 10, 100, 20, TRUE),
                    (102, 'OVER', 'Over Product', 10, 100, 20, TRUE),
                    (103, 'NORMAL', 'Normal Product', 10, 100, 20, TRUE),
                    (104, 'INACTIVE', 'Inactive Product', 10, 100, 20, FALSE);
                INSERT INTO stock_levels (product_id, warehouse_id, quantity, reserved_quantity) VALUES
                    (100, 100, 0, 0), (101, 100, 10, 2), (102, 100, 100, 5),
                    (103, 100, 50, 7), (104, 100, 0, 0);
                REFRESH MATERIALIZED VIEW mv_stock_summary;
                """);

        assertThat(names(schema,
                "SELECT sku || ':' || stock_status || ':' || available_quantity FROM mv_stock_summary ORDER BY sku"))
                .containsExactlyInAnyOrder("ZERO:OUT_OF_STOCK:0", "LOW:LOW_STOCK:8",
                        "OVER:OVERSTOCK:95", "NORMAL:NORMAL:43");
        assertThat(queryInt(schema, "SELECT count(*) FROM mv_stock_summary WHERE sku = 'INACTIVE'")).isZero();

        execute(schema, "REFRESH MATERIALIZED VIEW CONCURRENTLY mv_stock_summary");
    }

    @Test
    void repeatableSeedCanRerunWithoutDuplicatesOrUserDataLoss() throws Exception {
        String schema = migratedSchema();
        execute(schema, "INSERT INTO categories (id, name, code) VALUES (100, 'User Category', 'USER-CAT')");
        String repeatableSql = resourceText("db/migration/R__insert_dummy_category.sql");

        execute(schema, repeatableSql);
        execute(schema, repeatableSql);
        flyway(schema).migrate();

        assertThat(queryInt(schema, "SELECT count(*) FROM categories WHERE id BETWEEN 1 AND 10")).isEqualTo(10);
        assertThat(queryInt(schema, "SELECT count(*) FROM categories WHERE id = 100 AND code = 'USER-CAT'")).isEqualTo(1);
        assertThat(queryInt(schema, "SELECT count(*) FROM categories")).isEqualTo(11);
    }

    private static Flyway flyway(String schema) {
        return Flyway.configure()
                .dataSource(jdbcUrl(), username(), password())
                .locations(MIGRATION_LOCATION)
                .defaultSchema(schema)
                .schemas(schema)
                .load();
    }

    private static Flyway flyway(String schema, MigrationVersion target) {
        return Flyway.configure()
                .dataSource(jdbcUrl(), username(), password())
                .locations(MIGRATION_LOCATION)
                .defaultSchema(schema)
                .schemas(schema)
                .target(target)
                .load();
    }

    private static String migratedSchema() {
        String schema = newSchema();
        flyway(schema).migrate();
        return schema;
    }

    private static String newSchema() {
        return "migration_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static Connection connection(String schema) throws SQLException {
        Connection connection = java.sql.DriverManager.getConnection(jdbcUrl(), username(), password());
        connection.setSchema(schema);
        return connection;
    }

    private static String jdbcUrl() {
        return EXTERNAL_JDBC_URL != null ? EXTERNAL_JDBC_URL : POSTGRES.getJdbcUrl();
    }

    private static String username() {
        return EXTERNAL_JDBC_URL != null ? EXTERNAL_USERNAME : POSTGRES.getUsername();
    }

    private static String password() {
        return EXTERNAL_JDBC_URL != null ? EXTERNAL_PASSWORD : POSTGRES.getPassword();
    }

    private static void execute(String schema, String sql) throws SQLException {
        try (Connection connection = connection(schema); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryInt(String schema, String sql) throws SQLException {
        try (Connection connection = connection(schema);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String queryString(String schema, String sql) throws SQLException {
        try (Connection connection = connection(schema);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static boolean queryBoolean(String schema, String sql) throws SQLException {
        try (Connection connection = connection(schema);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private static Set<String> names(String schema, String sql) throws SQLException {
        Set<String> values = new HashSet<>();
        try (Connection connection = connection(schema);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;
    }

    private static List<String> columns(String schema, String table) throws SQLException {
        try (Connection connection = connection(schema);
             var statement = connection.prepareStatement("""
                     SELECT column_name FROM information_schema.columns
                     WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position
                     """)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet result = statement.executeQuery()) {
                var values = new java.util.ArrayList<String>();
                while (result.next()) {
                    values.add(result.getString(1));
                }
                return values;
            }
        }
    }

    private static void assertRejected(String schema, String sql) {
        assertThatThrownBy(() -> execute(schema, sql)).isInstanceOf(SQLException.class);
    }

    private static boolean matches(String schema, long productId, String term) throws SQLException {
        return queryBoolean(schema, "SELECT search_vector @@ plainto_tsquery('simple', '"
                + term.replace("'", "''") + "') FROM products WHERE id = " + productId);
    }

    private static void insertParents(String schema) throws SQLException {
        execute(schema, """
                INSERT INTO users (id, username, email, password) VALUES (100, 'owner', 'owner@example.com', 'hash');
                INSERT INTO warehouses (id, name, code) VALUES (100, 'Source', 'WH-100'), (101, 'Destination', 'WH-101');
                INSERT INTO products (id, sku, name) VALUES (100, 'SKU-100', 'Product 100');
                """);
    }

    private static String movement(String type, String status, String destination) {
        return "INSERT INTO stock_movements (reference_no, type, status, warehouse_id, dest_warehouse_id, created_by) "
                + "VALUES ('MOV-" + UUID.randomUUID() + "', '" + type + "', '" + status + "', 100, "
                + destination + ", 100)";
    }

    private static String alert(String type, int current, int threshold, String status, String resolvedAt) {
        return "INSERT INTO stock_alerts (product_id, warehouse_id, alert_type, current_quantity, threshold, status, resolved_at) "
                + "VALUES (100, 100, '" + type + "', " + current + ", " + threshold + ", '" + status + "', "
                + resolvedAt + ")";
    }

    private static String reorder(int suggested, int current, int point, String status) {
        return "INSERT INTO reorder_suggestions (product_id, warehouse_id, suggested_quantity, current_stock, reorder_point, status) "
                + "VALUES (100, 100, " + suggested + ", " + current + ", " + point + ", '" + status + "')";
    }

    private static String sha256(String resource) throws IOException, NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(resourceBytes(resource));
        return java.util.HexFormat.of().formatHex(digest);
    }

    private static String resourceText(String resource) throws IOException {
        return new String(resourceBytes(resource), StandardCharsets.UTF_8);
    }

    private static byte[] resourceBytes(String resource) throws IOException {
        try (InputStream stream = FlywayMigrationIntegrationTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + resource);
            }
            return stream.readAllBytes();
        }
    }
}
