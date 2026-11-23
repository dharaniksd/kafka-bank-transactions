package com.example.bank.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event written to Kafka. The transfer id is also the idempotency key
 * used by the consumer's inbox table.
 */
public record TransferRequested(
        UUID transferId,
        Long debitAccountId,
        Long creditAccountId,
        BigDecimal amount,
        Instant requestedAt
) {
}
