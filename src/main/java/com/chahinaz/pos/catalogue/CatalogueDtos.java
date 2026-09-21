package com.chahinaz.pos.catalogue;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CatalogueDtos {
  private CatalogueDtos() {}
  public record CategoryInput(@NotBlank @Size(max=120) String name, @Size(max=1000) String description, boolean active) {}
  public record CategoryView(UUID id, String name, String description, boolean active, Instant createdAt, Instant updatedAt) {
    static CategoryView of(Category c) { return new CategoryView(c.id, c.name, c.description, c.active, c.createdAt, c.updatedAt); }
  }
  public record ProductInput(
      @NotBlank @Size(max=80) String sku, @Size(max=80) String barcode,
      @NotBlank @Size(max=200) String name, @Size(max=2000) String description,
      @NotNull UUID categoryId,
      @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal costPriceUsd,
      @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal sellingPriceUsd,
      @Min(0) int lowStockThreshold, boolean featured, boolean newArrival) {}
  public record CreateProductInput(@NotNull @jakarta.validation.Valid ProductInput product, @Min(0) int initialStock) {}
  public record ProductView(UUID id, String sku, String barcode, String name, String description,
      UUID categoryId, String categoryName, BigDecimal costPriceUsd, BigDecimal sellingPriceUsd,
      int stockQuantity, int lowStockThreshold, boolean active, PublicationStatus publicationStatus,
      boolean featured, boolean newArrival, String imageUrl, Instant createdAt, Instant updatedAt) {
    static ProductView of(Product p) { return new ProductView(p.id,p.sku,p.barcode,p.name,p.description,p.category.id,p.category.name,
        p.costPriceUsd,p.sellingPriceUsd,p.stockQuantity,p.lowStockThreshold,p.active,p.publicationStatus,
        p.featured,p.newArrival,CatalogueDtos.imageUrl(p),p.createdAt,p.updatedAt); }
  }
  public record PublicProductView(UUID id, String sku, String name, String description, UUID categoryId,
      String categoryName, BigDecimal sellingPriceUsd, String imageUrl, boolean inStock,
      boolean featured, boolean newArrival) {
    static PublicProductView of(Product p) { return new PublicProductView(p.id,p.sku,p.name,p.description,p.category.id,
        p.category.name,p.sellingPriceUsd,CatalogueDtos.imageUrl(p),p.stockQuantity > 0,p.featured,p.newArrival); }
  }
  public record StockInput(@NotNull @Min(1) Integer quantity, @Size(max=500) String reason) {}
  public record AdjustmentInput(@NotNull Integer quantityChange, @NotBlank @Size(max=500) String reason) {}
  public record MovementView(UUID id, UUID productId, MovementType type, int quantityChange, int resultingQuantity,
      String reason, UUID actorEmployeeId, Instant occurredAt) {
    static MovementView of(InventoryMovement m) { return new MovementView(m.id,m.product.id,m.movementType,
        m.quantityChange,m.resultingQuantity,m.reason,m.actorEmployeeId,m.occurredAt); }
  }
  static String imageUrl(Product p) { return p.imageKey == null ? null : "/api/public/images/" + p.imageKey; }
}
