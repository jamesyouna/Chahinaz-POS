ALTER TABLE sale DROP CONSTRAINT sale_status_check;
ALTER TABLE sale ADD CONSTRAINT sale_status_check CHECK (status IN ('OPEN','HELD','COMPLETED','VOIDED'));
ALTER TABLE sale ADD COLUMN cashier_name_snapshot varchar(120);
UPDATE sale s SET cashier_name_snapshot=e.display_name FROM employee e WHERE e.id=s.employee_id;
ALTER TABLE sale ALTER COLUMN cashier_name_snapshot SET NOT NULL;
ALTER TABLE sale ADD COLUMN total_discount_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (total_discount_usd >= 0);
ALTER TABLE sale ADD COLUMN sale_discount_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (sale_discount_usd >= 0);
ALTER TABLE sale ADD COLUMN voided_at timestamptz;
ALTER TABLE sale ADD COLUMN voided_by uuid REFERENCES employee(id);
ALTER TABLE sale ADD COLUMN void_reason varchar(500);

ALTER TABLE sale_item RENAME COLUMN unit_price_usd TO effective_unit_price_usd;
ALTER TABLE sale_item ADD COLUMN original_unit_price_usd numeric(12,2);
UPDATE sale_item SET original_unit_price_usd=effective_unit_price_usd;
ALTER TABLE sale_item ALTER COLUMN original_unit_price_usd SET NOT NULL;
ALTER TABLE sale_item ADD COLUMN discount_usd numeric(12,2) NOT NULL DEFAULT 0 CHECK (discount_usd >= 0);
ALTER TABLE sale_item ADD COLUMN override_reason varchar(500);
ALTER TABLE sale_item ADD COLUMN override_requested_by uuid REFERENCES employee(id);
ALTER TABLE sale_item ADD COLUMN override_approved_by uuid REFERENCES employee(id);
ALTER TABLE sale_item ADD COLUMN override_at timestamptz;

CREATE TABLE register_session (
 id uuid PRIMARY KEY, employee_id uuid NOT NULL REFERENCES employee(id),
 status varchar(20) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
 opened_at timestamptz NOT NULL, closed_at timestamptz,
 opening_cash_usd numeric(12,2) NOT NULL CHECK (opening_cash_usd >= 0),
 opening_cash_lbp numeric(18,0) NOT NULL CHECK (opening_cash_lbp >= 0),
 expected_closing_cash_usd numeric(12,2), expected_closing_cash_lbp numeric(18,0),
 actual_closing_cash_usd numeric(12,2), actual_closing_cash_lbp numeric(18,0),
 difference_usd numeric(12,2), difference_lbp numeric(18,0), closing_note varchar(500)
);
CREATE UNIQUE INDEX register_one_open_per_employee ON register_session(employee_id) WHERE status='OPEN';
CREATE INDEX register_employee_time_idx ON register_session(employee_id,opened_at DESC);
ALTER TABLE sale ADD COLUMN register_session_id uuid REFERENCES register_session(id);

CREATE TABLE manager_approval (
 id uuid PRIMARY KEY, operation_type varchar(40) NOT NULL,
 requesting_employee_id uuid NOT NULL REFERENCES employee(id), approving_employee_id uuid NOT NULL REFERENCES employee(id),
 target_type varchar(40) NOT NULL, target_id uuid NOT NULL, reason varchar(500) NOT NULL,
 approved_at timestamptz NOT NULL, consumed_at timestamptz,
 CHECK (length(trim(reason)) >= 3)
);
CREATE INDEX approval_target_idx ON manager_approval(target_type,target_id,operation_type);

CREATE TABLE sale_return (
 id uuid PRIMARY KEY, original_sale_id uuid NOT NULL REFERENCES sale(id), employee_id uuid NOT NULL REFERENCES employee(id),
 register_session_id uuid REFERENCES register_session(id),
 refund_usd numeric(12,2) NOT NULL CHECK (refund_usd >= 0), refund_lbp numeric(18,0) NOT NULL CHECK (refund_lbp >= 0),
 refund_method varchar(30) NOT NULL CHECK (refund_method IN ('USD_CASH','LBP_CASH','WHISH_MANUAL')),
 whish_manually_recorded boolean NOT NULL DEFAULT false, reason varchar(500) NOT NULL, created_at timestamptz NOT NULL,
 CHECK (length(trim(reason)) >= 3)
);
CREATE INDEX sale_return_sale_idx ON sale_return(original_sale_id,created_at DESC);
CREATE TABLE sale_return_item (
 id uuid PRIMARY KEY, return_id uuid NOT NULL REFERENCES sale_return(id) ON DELETE CASCADE,
 sale_item_id uuid NOT NULL REFERENCES sale_item(id), quantity integer NOT NULL CHECK (quantity > 0),
 amount_usd numeric(12,2) NOT NULL CHECK (amount_usd >= 0), restockable boolean NOT NULL
);
CREATE INDEX return_item_sale_item_idx ON sale_return_item(sale_item_id);
ALTER TABLE inventory_movement ADD COLUMN return_id uuid REFERENCES sale_return(id);
