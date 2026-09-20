CREATE TABLE employee (
  id uuid PRIMARY KEY,
  username varchar(80) NOT NULL UNIQUE,
  display_name varchar(120) NOT NULL,
  password_hash varchar(255) NOT NULL,
  role varchar(20) NOT NULL CHECK (role IN ('ADMIN','MANAGER','TELLER')),
  enabled boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE store_setting (
  setting_key varchar(100) PRIMARY KEY,
  setting_value varchar(1000) NOT NULL,
  updated_by uuid REFERENCES employee(id),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE audit_event (
  id uuid PRIMARY KEY,
  actor_employee_id uuid REFERENCES employee(id),
  action varchar(80) NOT NULL,
  entity_type varchar(80) NOT NULL,
  entity_id varchar(100),
  before_value jsonb,
  after_value jsonb,
  occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_event_occurred_idx ON audit_event(occurred_at DESC);
CREATE INDEX audit_event_actor_idx ON audit_event(actor_employee_id, occurred_at DESC);
