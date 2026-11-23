package com.example.bank.producer.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull @Positive Long debitAccountId,
        @NotNull @Positive Long creditAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
