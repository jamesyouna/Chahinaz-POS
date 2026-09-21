CREATE SEQUENCE sale_number_seq START WITH 1;

CREATE TABLE sale (
  id uuid PRIMARY KEY,
  sale_number varchar(30) NOT NULL UNIQUE,
  employee_id uuid NOT NULL REFERENCES employee(id),
  status varchar(20) NOT NULL CHECK (status IN ('OPEN','COMPLETED','VOIDED')),
  subtotal_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (subtotal_usd >= 0),
  total_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (total_usd >= 0),
  exchange_rate numeric(14,2) CHECK (exchange_rate > 0),
  amount_paid_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (amount_paid_usd >= 0),
  amount_paid_lbp numeric(18,0) NOT NULL DEFAULT 0 CHECK (amount_paid_lbp >= 0),
  whish_amount_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (whish_amount_usd >= 0),
  change_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (change_usd >= 0),
  change_lbp numeric(18,0) NOT NULL DEFAULT 0 CHECK (change_lbp >= 0),
  note varchar(500),
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz
);
CREATE INDEX sale_employee_created_idx ON sale(employee_id, created_at DESC);
CREATE INDEX sale_status_created_idx ON sale(status, created_at DESC);

CREATE TABLE sale_item (
  id uuid PRIMARY KEY,
  sale_id uuid NOT NULL REFERENCES sale(id) ON DELETE CASCADE,
  product_id uuid NOT NULL REFERENCES product(id),
  sku_snapshot varchar(80) NOT NULL,
  product_name_snapshot varchar(200) NOT NULL,
  unit_price_usd numeric(12,2) NOT NULL CHECK (unit_price_usd >= 0),
  quantity integer NOT NULL CHECK (quantity > 0),
  line_total_usd numeric(12,2) NOT NULL CHECK (line_total_usd >= 0),
  UNIQUE(sale_id, product_id)
);
CREATE INDEX sale_item_sale_idx ON sale_item(sale_id);

CREATE TABLE payment (
  id uuid PRIMARY KEY,
  sale_id uuid NOT NULL REFERENCES sale(id) ON DELETE CASCADE,
  payment_type varchar(30) NOT NULL CHECK (payment_type IN ('USD_CASH','LBP_CASH','WHISH_MANUAL')),
  amount_usd numeric(12,2),
  amount_lbp numeric(18,0),
  manually_confirmed boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((payment_type='USD_CASH' AND amount_usd > 0 AND amount_lbp IS NULL) OR
         (payment_type='LBP_CASH' AND amount_lbp > 0 AND amount_usd IS NULL) OR
         (payment_type='WHISH_MANUAL' AND amount_usd > 0 AND amount_lbp IS NULL AND manually_confirmed))
);
CREATE INDEX payment_sale_idx ON payment(sale_id);

ALTER TABLE inventory_movement ADD COLUMN sale_id uuid REFERENCES sale(id);
CREATE INDEX inventory_movement_sale_idx ON inventory_movement(sale_id);
