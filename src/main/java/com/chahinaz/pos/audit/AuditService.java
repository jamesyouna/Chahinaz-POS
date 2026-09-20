package com.chahinaz.pos.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final JdbcTemplate jdbc;
  public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public void record(UUID actor, String action, String entityType, String entityId, String before, String after) {
    // Caller must pass only non-secret values; passwords and tokens are never audited.
    jdbc.update("INSERT INTO audit_event (id, actor_employee_id, action, entity_type, entity_id, before_value, after_value, occurred_at) VALUES (?, ?, ?, ?, ?, to_jsonb(?::text), to_jsonb(?::text), ?)",
        UUID.randomUUID(), actor, action, entityType, entityId, before, after, Timestamp.from(Instant.now()));
  }
}
