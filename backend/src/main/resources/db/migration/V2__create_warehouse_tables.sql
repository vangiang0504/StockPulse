CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    min_stock INT NOT NULL DEFAULT 10,
    max_stock INT NOT NULL DEFAULT 1000,
    reorder_point INT NOT NULL DEFAULT 20,
    reorder_quantity INT NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    search_vector TSVECTOR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

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
