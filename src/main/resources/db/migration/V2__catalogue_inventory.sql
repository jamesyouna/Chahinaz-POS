CREATE TABLE category (
  id uuid PRIMARY KEY,
  name varchar(120) NOT NULL,
  description varchar(1000),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT category_name_not_blank CHECK (length(trim(name)) > 0)
);
CREATE UNIQUE INDEX category_name_unique_ci ON category (lower(name));

CREATE TABLE product (
  id uuid PRIMARY KEY,
  sku varchar(80) NOT NULL UNIQUE,
  barcode varchar(80) UNIQUE,
  name varchar(200) NOT NULL,
  description varchar(2000),
  category_id uuid NOT NULL REFERENCES category(id),
  cost_price_usd numeric(12,2) NOT NULL CHECK (cost_price_usd >= 0),
  selling_price_usd numeric(12,2) NOT NULL CHECK (selling_price_usd >= 0),
  stock_quantity integer NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
  low_stock_threshold integer NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
  active boolean NOT NULL DEFAULT true,
  publication_status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (publication_status IN ('DRAFT','PUBLISHED','INACTIVE')),
  featured boolean NOT NULL DEFAULT false,
  new_arrival boolean NOT NULL DEFAULT false,
  image_key varchar(100),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT product_sku_not_blank CHECK (length(trim(sku)) > 0),
  CONSTRAINT product_name_not_blank CHECK (length(trim(name)) > 0),
  CONSTRAINT product_barcode_not_blank CHECK (barcode IS NULL OR length(trim(barcode)) > 0)
);
CREATE INDEX product_category_idx ON product(category_id);
CREATE INDEX product_public_idx ON product(publication_status, active, category_id);
CREATE INDEX product_stock_idx ON product(stock_quantity, low_stock_threshold);

CREATE TABLE inventory_movement (
  id uuid PRIMARY KEY,
  product_id uuid NOT NULL REFERENCES product(id),
  movement_type varchar(30) NOT NULL CHECK (movement_type IN ('INITIAL_STOCK','RESTOCK','SALE','RETURN','ADJUSTMENT_INCREASE','ADJUSTMENT_DECREASE')),
  quantity_change integer NOT NULL CHECK (quantity_change <> 0),
  resulting_quantity integer NOT NULL CHECK (resulting_quantity >= 0),
  reason varchar(500),
  actor_employee_id uuid REFERENCES employee(id),
  occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX inventory_movement_product_time_idx ON inventory_movement(product_id, occurred_at DESC);
