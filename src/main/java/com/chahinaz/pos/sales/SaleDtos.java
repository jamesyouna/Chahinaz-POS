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
 public record ItemView(UUID id,UUID productId,String sku,String name,BigDecimal unitPriceUsd,int quantity,BigDecimal lineTotalUsd){}
 public record PaymentView(PaymentType type,BigDecimal amountUsd,BigDecimal amountLbp,boolean manuallyConfirmed){}
 public record ReceiptView(UUID id,String saleNumber,SaleStatus status,Instant createdAt,Instant completedAt,String cashier,
   List<ItemView> items,BigDecimal subtotalUsd,BigDecimal totalUsd,BigDecimal exchangeRate,List<PaymentView> payments,
   BigDecimal changeUsd,BigDecimal changeLbp,String note){}
}
