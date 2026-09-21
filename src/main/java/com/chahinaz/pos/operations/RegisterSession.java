package com.chahinaz.pos.operations;
import com.chahinaz.pos.employee.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="register_session")
public class RegisterSession {
 @Id public UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") public Employee employee;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public RegisterStatus status;
 @Column(name="opened_at",nullable=false) public Instant openedAt; @Column(name="closed_at") public Instant closedAt;
 @Column(name="opening_cash_usd",nullable=false,precision=12,scale=2) public BigDecimal openingCashUsd;
 @Column(name="opening_cash_lbp",nullable=false,precision=18,scale=0) public BigDecimal openingCashLbp;
 @Column(name="expected_closing_cash_usd",precision=12,scale=2) public BigDecimal expectedClosingCashUsd;
 @Column(name="expected_closing_cash_lbp",precision=18,scale=0) public BigDecimal expectedClosingCashLbp;
 @Column(name="actual_closing_cash_usd",precision=12,scale=2) public BigDecimal actualClosingCashUsd;
 @Column(name="actual_closing_cash_lbp",precision=18,scale=0) public BigDecimal actualClosingCashLbp;
 @Column(name="difference_usd",precision=12,scale=2) public BigDecimal differenceUsd;
 @Column(name="difference_lbp",precision=18,scale=0) public BigDecimal differenceLbp;
 @Column(name="closing_note",length=500) public String closingNote;
 protected RegisterSession(){}
 public RegisterSession(Employee e,BigDecimal usd,BigDecimal lbp){id=UUID.randomUUID();employee=e;status=RegisterStatus.OPEN;openedAt=Instant.now();openingCashUsd=usd;openingCashLbp=lbp;}
}
