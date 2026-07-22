UPDATE products
SET search_vector =
    setweight(to_tsvector('simple', coalesce(sku, '')), 'A')
    || setweight(to_tsvector('simple', coalesce(name, '')), 'B');

CREATE FUNCTION products_search_vector_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.sku, '')), 'A')
        || setweight(to_tsvector('simple', coalesce(NEW.name, '')), 'B');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_products_search_vector_update
BEFORE INSERT OR UPDATE OF sku, name ON products
FOR EACH ROW
EXECUTE FUNCTION products_search_vector_update();

ALTER TABLE products ALTER COLUMN search_vector SET NOT NULL;

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_search ON products USING GIN(search_vector);
CREATE INDEX idx_stock_product ON stock_levels(product_id);
CREATE INDEX idx_stock_warehouse ON stock_levels(warehouse_id);
CREATE INDEX idx_stock_low ON stock_levels(quantity) WHERE quantity < 20;
CREATE INDEX idx_movements_warehouse ON stock_movements(warehouse_id);
CREATE INDEX idx_movements_type_status ON stock_movements(type, status);
CREATE INDEX idx_movements_created ON stock_movements(created_at);
CREATE INDEX idx_movement_items_product ON stock_movement_items(product_id);
CREATE INDEX idx_alerts_active ON stock_alerts(status) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_stock_alert_unresolved
    ON stock_alerts(product_id, warehouse_id, alert_type)
    WHERE status IN ('ACTIVE', 'ACKNOWLEDGED');
CREATE UNIQUE INDEX uk_reorder_suggestion_pending
    ON reorder_suggestions(product_id, warehouse_id)
    WHERE status = 'PENDING';
