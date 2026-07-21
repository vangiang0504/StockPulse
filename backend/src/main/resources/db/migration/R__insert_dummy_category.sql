INSERT INTO categories (id, name, code, parent_id) VALUES
    (1, 'Smart Phone', 'CAT-SP', NULL),
    (2, 'Electronics', 'CAT-ELEC', NULL),
    (5, 'Accessories', 'CAT-ACC', NULL),
    (8, 'Networking', 'CAT-NET', NULL),
    (3, 'Laptops', 'CAT-LAP', 2),
    (4, 'Tablets', 'CAT-TAB', 2),
    (6, 'Chargers', 'CAT-CHG', 5),
    (7, 'Headphones', 'CAT-AUD', 5),
    (9, 'Routers', 'CAT-RTR', 8),
    (10, 'Switches', 'CAT-SWT', 8)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    parent_id = EXCLUDED.parent_id;

SELECT setval('categories_id_seq', COALESCE((SELECT MAX(id) FROM categories), 1));
