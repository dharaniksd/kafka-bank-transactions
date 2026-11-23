package com.example.bank.consumer.api;

import java.math.BigDecimal;

public record AccountResponse(Long id, String accountNumber, BigDecimal balance) {
}
