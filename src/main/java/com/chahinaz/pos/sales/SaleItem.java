package com.chahinaz.pos.sales;
import com.chahinaz.pos.catalogue.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="sale_item")
public class SaleItem {
 @Id public UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") public Sale sale;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") public Product product;
 @Column(name="sku_snapshot",nullable=false,length=80) public String skuSnapshot;
 @Column(name="product_name_snapshot",nullable=false,length=200) public String productNameSnapshot;
 @Column(name="original_unit_price_usd",nullable=false,precision=12,scale=2) public BigDecimal originalUnitPriceUsd;
 @Column(name="effective_unit_price_usd",nullable=false,precision=12,scale=2) public BigDecimal unitPriceUsd;
 @Column(name="discount_usd",nullable=false,precision=12,scale=2) public BigDecimal discountUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="override_reason",length=500) public String overrideReason;
 @Column(name="override_requested_by") public UUID overrideRequestedBy;
 @Column(name="override_approved_by") public UUID overrideApprovedBy;
 @Column(name="override_at") public java.time.Instant overrideAt;
 @Column(nullable=false) public int quantity;
 @Column(name="line_total_usd",nullable=false,precision=12,scale=2) public BigDecimal lineTotalUsd;
 protected SaleItem() {}
 public SaleItem(Sale s,Product p,int q){id=UUID.randomUUID();sale=s;product=p;skuSnapshot=p.sku;productNameSnapshot=p.name;originalUnitPriceUsd=p.sellingPriceUsd;unitPriceUsd=originalUnitPriceUsd;setQuantity(q);}
 public void setQuantity(int q){quantity=q;recalculate();}
 public void recalculate(){lineTotalUsd=unitPriceUsd.multiply(BigDecimal.valueOf(quantity)).subtract(discountUsd).setScale(2);}
 public Product getProduct(){return product;}
 public int getQuantity(){return quantity;}
}
