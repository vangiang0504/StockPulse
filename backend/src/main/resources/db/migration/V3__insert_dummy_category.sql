INSERT INTO categories (id, name, code, parent_id) VALUES 
(1, 'Smart Phone', 'CAT-SP', NULL),
(2, 'Electronics', 'CAT-ELEC', NULL),
(3, 'Laptops', 'CAT-LAP', 2),
(4, 'Tablets', 'CAT-TAB', 2),
(5, 'Accessories', 'CAT-ACC', NULL),
(6, 'Chargers', 'CAT-CHG', 5),
(7, 'Headphones', 'CAT-AUD', 5),
(8, 'Networking', 'CAT-NET', NULL),
(9, 'Routers', 'CAT-RTR', 8),
(10, 'Switches', 'CAT-SWT', 8)
ON CONFLICT (id) DO NOTHING;

-- Update the auto-increment sequence so future API creations don't fail
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
