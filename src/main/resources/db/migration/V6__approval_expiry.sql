ALTER TABLE manager_approval ADD COLUMN expires_at timestamptz;
UPDATE manager_approval SET expires_at=approved_at + interval '5 minutes';
ALTER TABLE manager_approval ALTER COLUMN expires_at SET NOT NULL;
CREATE INDEX manager_approval_expiry_idx ON manager_approval(expires_at) WHERE consumed_at IS NULL;
