package com.chahinaz.pos.employee;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "employee")
public class Employee {
  @Id public UUID id;
  @Column(nullable = false, unique = true, length = 80) public String username;
  @Column(name = "display_name", nullable = false, length = 120) public String displayName;
  @Column(name = "password_hash", nullable = false) public String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) public Role role;
  @Column(nullable = false) public boolean enabled = true;
  @Column(name = "created_at", nullable = false) public Instant createdAt;
  @Column(name = "updated_at", nullable = false) public Instant updatedAt;

  protected Employee() {}
  public Employee(String username, String displayName, String passwordHash, Role role) {
    this.id = UUID.randomUUID(); this.username = username; this.displayName = displayName;
    this.passwordHash = passwordHash; this.role = role;
    this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
  }
  public UUID getId() { return id; }
  public String getUsername() { return username; }
  public String getDisplayName() { return displayName; }
}
