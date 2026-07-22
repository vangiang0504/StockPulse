CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    reference_no VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    dest_warehouse_id BIGINT REFERENCES warehouses(id),
    notes TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id),
    approved_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_movement_type CHECK (type IN ('IMPORT', 'EXPORT', 'TRANSFER', 'ADJUSTMENT')),
    CONSTRAINT chk_movement_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_movement_destination CHECK (
        (type = 'TRANSFER' AND dest_warehouse_id IS NOT NULL AND dest_warehouse_id <> warehouse_id)
        OR (type <> 'TRANSFER' AND dest_warehouse_id IS NULL)
    )
);

CREATE TABLE stock_movement_items (
    id BIGSERIAL PRIMARY KEY,
    movement_id BIGINT NOT NULL REFERENCES stock_movements(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2),
    batch_number VARCHAR(50),
    expiry_date DATE,
    notes TEXT,
    CONSTRAINT chk_movement_item_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_movement_item_unit_cost_non_negative CHECK (unit_cost IS NULL OR unit_cost >= 0)
);
