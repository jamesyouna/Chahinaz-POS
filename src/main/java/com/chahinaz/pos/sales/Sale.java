package com.chahinaz.pos.sales;
import com.chahinaz.pos.employee.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="sale")
public class Sale {
 @Id public UUID id;
 @Column(name="sale_number",nullable=false,length=30) public String saleNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") public Employee employee;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public SaleStatus status=SaleStatus.OPEN;
 @Column(name="subtotal_usd",nullable=false,precision=12,scale=2) public BigDecimal subtotalUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="total_usd",nullable=false,precision=12,scale=2) public BigDecimal totalUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="exchange_rate",precision=14,scale=2) public BigDecimal exchangeRate;
 @Column(name="amount_paid_usd",nullable=false,precision=12,scale=2) public BigDecimal amountPaidUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="amount_paid_lbp",nullable=false,precision=18,scale=0) public BigDecimal amountPaidLbp=BigDecimal.ZERO.setScale(0);
 @Column(name="whish_amount_usd",nullable=false,precision=12,scale=2) public BigDecimal whishAmountUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="change_usd",nullable=false,precision=12,scale=2) public BigDecimal changeUsd=BigDecimal.ZERO.setScale(2);
 @Column(name="change_lbp",nullable=false,precision=18,scale=0) public BigDecimal changeLbp=BigDecimal.ZERO.setScale(0);
 @Column(length=500) public String note;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="completed_at") public Instant completedAt;
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id") public List<SaleItem> items=new ArrayList<>();
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) public List<Payment> payments=new ArrayList<>();
 protected Sale() {}
 public Sale(String number,Employee employee,String note){id=UUID.randomUUID();saleNumber=number;this.employee=employee;this.note=note;createdAt=Instant.now();}
}
