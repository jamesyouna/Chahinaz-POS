package com.chahinaz.pos.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "category")
public class Category {
  @Id public UUID id;
  @Column(nullable = false, length = 120) public String name;
  @Column(length = 1000) public String description;
  @Column(nullable = false) public boolean active = true;
  @Column(name = "created_at", nullable = false) public Instant createdAt;
  @Column(name = "updated_at", nullable = false) public Instant updatedAt;
  protected Category() {}
  public Category(String name, String description) {
    id = UUID.randomUUID(); this.name = name; this.description = description;
    createdAt = Instant.now(); updatedAt = createdAt;
  }
  public UUID getId() { return id; }
  public boolean isActive() { return active; }
}
