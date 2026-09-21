package com.chahinaz.pos.catalogue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  boolean existsBySku(String sku);
  boolean existsByBarcode(String barcode);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id = :id") Optional<Product> findLockedById(UUID id);
  List<Product> findByPublicationStatusAndActiveTrueAndCategoryActiveTrueOrderByNameAsc(PublicationStatus status);
  Optional<Product> findByIdAndPublicationStatusAndActiveTrueAndCategoryActiveTrue(UUID id, PublicationStatus status);
  @Query("select p from Product p where p.stockQuantity <= p.lowStockThreshold order by p.name") List<Product> lowStock();
  List<Product> findByStockQuantityOrderByNameAsc(int quantity);
  List<Product> findByActiveTrueAndCategoryActiveTrueOrderByNameAsc();
  boolean existsByImageKeyAndPublicationStatusAndActiveTrueAndCategoryActiveTrue(String key, PublicationStatus status);
}
