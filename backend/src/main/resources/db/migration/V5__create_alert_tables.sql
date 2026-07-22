CREATE TABLE stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    alert_type VARCHAR(20) NOT NULL,
    current_quantity INT NOT NULL,
    threshold INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    CONSTRAINT chk_stock_alert_type CHECK (alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK', 'OVERSTOCK')),
    CONSTRAINT chk_stock_alert_status CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_stock_alert_current_quantity_non_negative CHECK (current_quantity >= 0),
    CONSTRAINT chk_stock_alert_threshold_non_negative CHECK (threshold >= 0),
    CONSTRAINT chk_stock_alert_resolution CHECK (
        (status = 'RESOLVED' AND resolved_at IS NOT NULL)
        OR (status <> 'RESOLVED' AND resolved_at IS NULL)
    )
);

CREATE TABLE reorder_suggestions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    suggested_quantity INT NOT NULL,
    current_stock INT NOT NULL,
    reorder_point INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reorder_suggested_quantity_positive CHECK (suggested_quantity >= 1),
    CONSTRAINT chk_reorder_current_stock_non_negative CHECK (current_stock >= 0),
    CONSTRAINT chk_reorder_point_non_negative CHECK (reorder_point >= 0),
    CONSTRAINT chk_reorder_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
