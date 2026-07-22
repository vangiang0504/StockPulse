CREATE TABLE stock_levels (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT chk_stock_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_stock_reserved_quantity_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_stock_version_non_negative CHECK (version >= 0)
);
