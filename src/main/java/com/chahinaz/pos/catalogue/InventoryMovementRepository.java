package com.chahinaz.pos.catalogue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
  List<InventoryMovement> findByProductIdOrderByOccurredAtDesc(UUID productId);
}
