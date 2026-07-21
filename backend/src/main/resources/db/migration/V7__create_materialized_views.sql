CREATE MATERIALIZED VIEW mv_stock_summary AS
SELECT
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    w.id AS warehouse_id,
    w.name AS warehouse_name,
    sl.quantity,
    sl.reserved_quantity,
    sl.quantity - sl.reserved_quantity AS available_quantity,
    p.min_stock,
    p.reorder_point,
    CASE
        WHEN sl.quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN sl.quantity <= p.min_stock THEN 'LOW_STOCK'
        WHEN sl.quantity >= p.max_stock THEN 'OVERSTOCK'
        ELSE 'NORMAL'
    END AS stock_status
FROM stock_levels sl
JOIN products p ON p.id = sl.product_id
JOIN warehouses w ON w.id = sl.warehouse_id
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.active = TRUE;

CREATE UNIQUE INDEX idx_mv_stock_summary
    ON mv_stock_summary(product_id, warehouse_id);
