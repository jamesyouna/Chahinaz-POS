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
 @Column(name="unit_price_usd",nullable=false,precision=12,scale=2) public BigDecimal unitPriceUsd;
 @Column(nullable=false) public int quantity;
 @Column(name="line_total_usd",nullable=false,precision=12,scale=2) public BigDecimal lineTotalUsd;
 protected SaleItem() {}
 public SaleItem(Sale s,Product p,int q){id=UUID.randomUUID();sale=s;product=p;skuSnapshot=p.sku;productNameSnapshot=p.name;unitPriceUsd=p.sellingPriceUsd;setQuantity(q);}
 public void setQuantity(int q){quantity=q;lineTotalUsd=unitPriceUsd.multiply(BigDecimal.valueOf(q)).setScale(2);}
 public Product getProduct(){return product;}
 public int getQuantity(){return quantity;}
}
