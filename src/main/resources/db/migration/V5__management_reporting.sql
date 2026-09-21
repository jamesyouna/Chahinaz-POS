ALTER TABLE sale_item ADD COLUMN cost_price_usd_snapshot numeric(12,2);
ALTER TABLE sale_item ADD COLUMN category_id_snapshot uuid;
ALTER TABLE sale_item ADD COLUMN category_name_snapshot varchar(120);
ALTER TABLE sale_item ADD COLUMN discount_applied_by uuid REFERENCES employee(id);
ALTER TABLE sale_item ADD COLUMN discount_applied_at timestamptz;
ALTER TABLE sale ADD COLUMN discount_applied_by uuid REFERENCES employee(id);
ALTER TABLE sale ADD COLUMN discount_applied_at timestamptz;

UPDATE sale_item si SET
 cost_price_usd_snapshot=p.cost_price_usd,
 category_id_snapshot=c.id,
 category_name_snapshot=c.name
FROM product p JOIN category c ON c.id=p.category_id
WHERE p.id=si.product_id;

ALTER TABLE sale_item ALTER COLUMN cost_price_usd_snapshot SET NOT NULL;
ALTER TABLE sale_item ALTER COLUMN category_id_snapshot SET NOT NULL;
ALTER TABLE sale_item ALTER COLUMN category_name_snapshot SET NOT NULL;
ALTER TABLE sale_item ADD CONSTRAINT sale_item_cost_snapshot_nonnegative CHECK (cost_price_usd_snapshot >= 0);

CREATE INDEX sale_completed_reporting_idx ON sale(completed_at DESC) WHERE status='COMPLETED';
CREATE INDEX sale_voided_reporting_idx ON sale(voided_at DESC) WHERE status='VOIDED';
CREATE INDEX sale_item_product_reporting_idx ON sale_item(product_id,sale_id);
CREATE INDEX sale_item_category_reporting_idx ON sale_item(category_id_snapshot,sale_id);
CREATE INDEX payment_type_reporting_idx ON payment(payment_type,sale_id);
CREATE INDEX sale_return_created_reporting_idx ON sale_return(created_at DESC);
CREATE INDEX inventory_movement_occurred_reporting_idx ON inventory_movement(occurred_at DESC,product_id);
