package com.chahinaz.pos.settings;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "store_setting")
public class StoreSetting {
  @Id @Column(name = "setting_key", length = 100) public String key;
  @Column(name = "setting_value", nullable = false, length = 1000) public String value;
  @Column(name = "updated_by") public UUID updatedBy;
  @Column(name = "updated_at", nullable = false) public Instant updatedAt;
  protected StoreSetting() {}
  public StoreSetting(String key, String value, UUID updatedBy) {
    this.key = key; this.value = value; this.updatedBy = updatedBy; this.updatedAt = Instant.now();
  }
}
