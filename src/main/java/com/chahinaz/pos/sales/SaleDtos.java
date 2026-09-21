package com.chahinaz.pos.sales;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public final class SaleDtos {
 private SaleDtos(){}
 public record CreateSaleInput(@Size(max=500) String note){}
 public record AddItemInput(@NotNull UUID productId,@Min(1) int quantity){}
 public record QuantityInput(@Min(1) int quantity){}
 public record CompleteSaleInput(@DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal paidUsd,
   @DecimalMin("0") @Digits(integer=18,fraction=0) BigDecimal paidLbp,
   @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal whishUsd,Boolean whishManuallyConfirmed){}
 public record ReasonInput(@NotBlank @Size(min=3,max=500) String reason){}
 public record DiscountInput(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amountUsd,UUID approvalId,String reason){}
 public record PriceOverrideInput(@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal unitPriceUsd,@NotBlank @Size(min=3,max=500) String reason,UUID approvalId){}
 public record ItemView(UUID id,UUID productId,String sku,String name,BigDecimal originalUnitPriceUsd,BigDecimal unitPriceUsd,BigDecimal discountUsd,int quantity,BigDecimal lineTotalUsd,String overrideReason,UUID overrideApprovedBy){}
 public record PaymentView(PaymentType type,BigDecimal amountUsd,BigDecimal amountLbp,boolean manuallyConfirmed){}
 public record ReceiptView(UUID id,String saleNumber,SaleStatus status,Instant createdAt,Instant completedAt,String cashier,
   List<ItemView> items,BigDecimal subtotalUsd,BigDecimal totalDiscountUsd,BigDecimal totalUsd,BigDecimal exchangeRate,List<PaymentView> payments,
   BigDecimal changeUsd,BigDecimal changeLbp,String note,Instant voidedAt,String voidReason,List<ReturnView> returns){}
 public record ReturnItemView(UUID saleItemId,int quantity,BigDecimal amountUsd,boolean restockable){}
 public record ReturnView(UUID id,BigDecimal refundUsd,BigDecimal refundLbp,String method,String reason,Instant createdAt,List<ReturnItemView> items){}
}
