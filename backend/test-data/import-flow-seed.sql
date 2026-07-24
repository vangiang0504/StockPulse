BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM warehouses
        WHERE id = 99101
          AND code <> 'WH-IMPORT-TEST'
    ) THEN
        RAISE EXCEPTION
            'Seed ID 99101 is already used by a non-test warehouse';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM products
        WHERE id = 99201
          AND sku <> 'IMP-TEST-001'
    ) OR EXISTS (
        SELECT 1
        FROM products
        WHERE id = 99202
          AND sku <> 'IMP-TEST-002'
    ) THEN
        RAISE EXCEPTION
            'Seed product IDs 99201/99202 are already used by non-test products';
    END IF;
END
$$;

CREATE TEMP TABLE import_seed_warehouses ON COMMIT DROP AS
SELECT id
FROM warehouses
WHERE id = 99101
   OR code = 'WH-IMPORT-TEST';

CREATE TEMP TABLE import_seed_products ON COMMIT DROP AS
SELECT id
FROM products
WHERE id IN (99201, 99202)
   OR sku IN ('IMP-TEST-001', 'IMP-TEST-002');

CREATE TEMP TABLE import_seed_movements ON COMMIT DROP AS
SELECT DISTINCT sm.id
FROM stock_movements sm
WHERE sm.warehouse_id IN (SELECT id FROM import_seed_warehouses)
   OR sm.dest_warehouse_id IN (SELECT id FROM import_seed_warehouses)
   OR EXISTS (
       SELECT 1
       FROM stock_movement_items smi
       WHERE smi.movement_id = sm.id
         AND smi.product_id IN (SELECT id FROM import_seed_products)
   );

DELETE FROM stock_movement_items
WHERE movement_id IN (SELECT id FROM import_seed_movements)
   OR product_id IN (SELECT id FROM import_seed_products);

DELETE FROM stock_movements
WHERE id IN (SELECT id FROM import_seed_movements);

DELETE FROM reorder_suggestions
WHERE product_id IN (SELECT id FROM import_seed_products)
   OR warehouse_id IN (SELECT id FROM import_seed_warehouses);

DELETE FROM stock_alerts
WHERE product_id IN (SELECT id FROM import_seed_products)
   OR warehouse_id IN (SELECT id FROM import_seed_warehouses);

DELETE FROM stock_levels
WHERE product_id IN (SELECT id FROM import_seed_products)
   OR warehouse_id IN (SELECT id FROM import_seed_warehouses);

DELETE FROM products
WHERE id IN (SELECT id FROM import_seed_products);

DELETE FROM warehouses
WHERE id IN (SELECT id FROM import_seed_warehouses);

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    role,
    active,
    created_at,
    updated_at
)
VALUES
    (
        'import_staff',
        'import.staff@stockpulse.test',
        '$2a$10$LxLTs6HdI7grHw5.AnwfkuCQ/.sr.mOUCf/ihiAvup/EVaSOUSrru',
        'Import Flow Staff',
        'STAFF',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        'import_manager',
        'import.manager@stockpulse.test',
        '$2a$10$LxLTs6HdI7grHw5.AnwfkuCQ/.sr.mOUCf/ihiAvup/EVaSOUSrru',
        'Import Flow Manager',
        'MANAGER',
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (username) DO UPDATE
SET email = EXCLUDED.email,
    password = EXCLUDED.password,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO warehouses (
    id,
    name,
    code,
    address,
    active,
    created_at,
    updated_at
)
VALUES (
    99101,
    'Import Flow Test Warehouse',
    'WH-IMPORT-TEST',
    'Postman integration test data',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO products (
    id,
    sku,
    name,
    description,
    category_id,
    unit,
    min_stock,
    max_stock,
    reorder_point,
    reorder_quantity,
    active,
    created_at,
    updated_at
)
VALUES
    (
        99201,
        'IMP-TEST-001',
        'Import Test Product A',
        'Dedicated product for the Postman import lifecycle',
        2,
        'PCS',
        5,
        500,
        20,
        50,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        99202,
        'IMP-TEST-002',
        'Import Test Product B',
        'Dedicated product for the Postman import lifecycle',
        2,
        'PCS',
        2,
        200,
        5,
        20,
        TRUE,
        NOW(),
        NOW()
    );

INSERT INTO stock_levels (
    product_id,
    warehouse_id,
    quantity,
    reserved_quantity,
    version,
    updated_at
)
VALUES
    (99201, 99101, 10, 0, 0, NOW()),
    (99202, 99101, 0, 0, 0, NOW());

SELECT setval(
    'users_id_seq',
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM users), 1),
    TRUE);
SELECT setval(
    'warehouses_id_seq',
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM warehouses), 1),
    TRUE);
SELECT setval(
    'products_id_seq',
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM products), 1),
    TRUE);

REFRESH MATERIALIZED VIEW mv_stock_summary;

COMMIT;

SELECT
    w.id AS warehouse_id,
    w.code AS warehouse_code,
    p.id AS product_id,
    p.sku,
    sl.quantity AS baseline_quantity,
    p.reorder_point,
    p.reorder_quantity
FROM stock_levels sl
JOIN warehouses w ON w.id = sl.warehouse_id
JOIN products p ON p.id = sl.product_id
WHERE w.code = 'WH-IMPORT-TEST'
ORDER BY p.id;
