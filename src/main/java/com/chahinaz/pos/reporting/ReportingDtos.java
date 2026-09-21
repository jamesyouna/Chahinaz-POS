package com.chahinaz.pos.reporting;
import java.math.BigDecimal;import java.time.*;import java.util.*;
public final class ReportingDtos {private ReportingDtos(){}
 public record Range(LocalDate from,LocalDate to,String timezone){}
 public record Comparison(BigDecimal current,BigDecimal previous,BigDecimal absoluteDifference,BigDecimal percentageChange){}
 public record Dashboard(Range range,BigDecimal grossSales,BigDecimal netSales,long completedTransactions,BigDecimal averageTransactionValue,long unitsSold,BigDecimal totalDiscounts,BigDecimal totalRefunds,BigDecimal netRevenueAfterRefunds,BigDecimal costOfGoodsSold,BigDecimal grossProfit,BigDecimal grossMarginPercent,BigDecimal cashUsdReceived,BigDecimal cashLbpReceived,BigDecimal whishReceived,long returnsCount,long voidedSales,long heldSales,long lowStockProducts,long outOfStockProducts,Comparison netRevenueComparison){}
 public record SalesPeriod(String period,long salesCount,long unitsSold,BigDecimal grossSales,BigDecimal discounts,BigDecimal refunds,BigDecimal netSales,BigDecimal averageTransactionValue,BigDecimal grossProfit){}
 public record ProductPerformance(UUID productId,String sku,String productName,long unitsSold,BigDecimal grossRevenue,BigDecimal discounts,BigDecimal netRevenue,long quantityReturned,BigDecimal refundAmount,BigDecimal costOfGoodsSold,BigDecimal grossProfit,BigDecimal grossMarginPercent,int currentStock){}
 public record CategoryPerformance(UUID categoryId,String categoryName,long unitsSold,BigDecimal grossRevenue,BigDecimal netRevenue,BigDecimal refunds,BigDecimal grossProfit,BigDecimal shareOfStoreSalesPercent){}
 public record InventoryRow(UUID productId,String sku,String productName,String category,int stock,int lowStockThreshold,String stockStatus,boolean active,BigDecimal currentUnitCost,BigDecimal inventoryValue,long unitsSold,long unitsRestocked,long nonRestockableReturns){}
 public record PaymentSummary(BigDecimal usdCash,BigDecimal lbpCash,BigDecimal whishUsd,long usdOnlySales,long lbpOnlySales,long whishOnlySales,long mixedSales){}
 public record RegisterReport(UUID id,UUID employeeId,String employeeName,Instant openedAt,Instant closedAt,String status,BigDecimal openingUsd,BigDecimal openingLbp,BigDecimal expectedUsd,BigDecimal expectedLbp,BigDecimal actualUsd,BigDecimal actualLbp,BigDecimal differenceUsd,BigDecimal differenceLbp,long transactionCount,BigDecimal cashSalesUsd,BigDecimal cashSalesLbp,BigDecimal refundsUsd,BigDecimal refundsLbp){}
 public record ReturnReport(UUID returnId,String saleNumber,String sku,String productName,int quantity,BigDecimal amountUsd,boolean restockable,String employee,String reason,Instant occurredAt){}
 public record DiscountReport(String saleNumber,String sku,String productName,BigDecimal amount,BigDecimal percentage,String employee,UUID approvalId,Instant occurredAt){}
 public record OverrideReport(String saleNumber,String sku,String productName,BigDecimal originalPrice,BigDecimal effectivePrice,BigDecimal difference,String requestingEmployee,String approvingEmployee,String reason,Instant occurredAt){}
 public record VoidReport(String saleNumber,String employee,String reason,Instant occurredAt){}
 public record MovementReport(UUID id,String sku,String productName,String type,int quantityChange,int resultingQuantity,String reason,UUID saleId,UUID returnId,Instant occurredAt){}
}
