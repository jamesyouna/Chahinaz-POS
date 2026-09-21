package com.chahinaz.pos.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "inventory_movement")
public class InventoryMovement {
  @Id public UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") public Product product;
  @Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false, length = 30) public MovementType movementType;
  @Column(name = "quantity_change", nullable = false) public int quantityChange;
  @Column(name = "resulting_quantity", nullable = false) public int resultingQuantity;
  @Column(length = 500) public String reason;
  @Column(name = "actor_employee_id") public UUID actorEmployeeId;
  @Column(name = "sale_id") public UUID saleId;
  @Column(name = "return_id") public UUID returnId;
  @Column(name = "occurred_at", nullable = false) public Instant occurredAt;
  protected InventoryMovement() {}
  public InventoryMovement(Product product, MovementType type, int change, int resulting, String reason, UUID actor) {
    id = UUID.randomUUID(); this.product = product; movementType = type; quantityChange = change;
    resultingQuantity = resulting; this.reason = reason; actorEmployeeId = actor; occurredAt = Instant.now();
  }
  public InventoryMovement(Product product, MovementType type, int change, int resulting, String reason, UUID actor, UUID saleId) {
    this(product,type,change,resulting,reason,actor); this.saleId=saleId;
  }
  public InventoryMovement forReturn(UUID returnId) { this.returnId=returnId; return this; }
}
