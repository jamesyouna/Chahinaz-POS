package com.chahinaz.pos.sales;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payment")
public class Payment {
 @Id public UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") public Sale sale;
 @Enumerated(EnumType.STRING) @Column(name="payment_type",nullable=false,length=30) public PaymentType type;
 @Column(name="amount_usd",precision=12,scale=2) public BigDecimal amountUsd;
 @Column(name="amount_lbp",precision=18,scale=0) public BigDecimal amountLbp;
 @Column(name="manually_confirmed",nullable=false) public boolean manuallyConfirmed;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected Payment() {}
 public Payment(Sale s,PaymentType type,BigDecimal usd,BigDecimal lbp,boolean confirmed){id=UUID.randomUUID();sale=s;this.type=type;amountUsd=usd;amountLbp=lbp;manuallyConfirmed=confirmed;createdAt=Instant.now();}
}
