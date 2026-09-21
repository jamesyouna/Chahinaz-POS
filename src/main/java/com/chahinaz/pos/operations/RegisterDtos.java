package com.chahinaz.pos.operations;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
public final class RegisterDtos {private RegisterDtos(){}
 public record OpenInput(@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal openingCashUsd,@NotNull @DecimalMin("0") @Digits(integer=18,fraction=0) BigDecimal openingCashLbp){}
 public record CloseInput(@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal actualCashUsd,@NotNull @DecimalMin("0") @Digits(integer=18,fraction=0) BigDecimal actualCashLbp,@Size(max=500) String note){}
 public record View(UUID id,UUID employeeId,String employeeName,RegisterStatus status,Instant openedAt,Instant closedAt,BigDecimal openingCashUsd,BigDecimal openingCashLbp,BigDecimal expectedCashUsd,BigDecimal expectedCashLbp,BigDecimal actualCashUsd,BigDecimal actualCashLbp,BigDecimal differenceUsd,BigDecimal differenceLbp,String closingNote){}
}
