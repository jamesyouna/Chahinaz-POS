package com.chahinaz.pos.catalogue;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "product")
public class Product {
  @Id public UUID id;
  @Column(nullable = false, length = 80) public String sku;
  @Column(length = 80) public String barcode;
  @Column(nullable = false, length = 200) public String name;
  @Column(length = 2000) public String description;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id") public Category category;
  @Column(name = "cost_price_usd", nullable = false, precision = 12, scale = 2) public BigDecimal costPriceUsd;
  @Column(name = "selling_price_usd", nullable = false, precision = 12, scale = 2) public BigDecimal sellingPriceUsd;
  @Column(name = "stock_quantity", nullable = false) public int stockQuantity;
  @Column(name = "low_stock_threshold", nullable = false) public int lowStockThreshold;
  @Column(nullable = false) public boolean active = true;
  @Enumerated(EnumType.STRING) @Column(name = "publication_status", nullable = false, length = 20) public PublicationStatus publicationStatus = PublicationStatus.DRAFT;
  @Column(nullable = false) public boolean featured;
  @Column(name = "new_arrival", nullable = false) public boolean newArrival;
  @Column(name = "image_key", length = 100) public String imageKey;
  @Column(name = "created_at", nullable = false) public Instant createdAt;
  @Column(name = "updated_at", nullable = false) public Instant updatedAt;
  protected Product() {}
  public Product(String sku, Category category) {
    id = UUID.randomUUID(); this.sku = sku; this.category = category;
    createdAt = Instant.now(); updatedAt = createdAt;
  }
  public UUID getId() { return id; }
  public Category getCategory() { return category; }
  public boolean isActive() { return active; }
  public int getStockQuantity() { return stockQuantity; }
  public void deductStock(int quantity) {
    if (quantity <= 0 || quantity > stockQuantity) throw new IllegalArgumentException("Invalid stock deduction");
    stockQuantity -= quantity;
    updatedAt = Instant.now();
  }
}
