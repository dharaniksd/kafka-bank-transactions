package com.example.bank.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Inbox row that makes database application of a Kafka event idempotent. */
@Entity
@Table(name = "processed_transfers")
public class ProcessedTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false, unique = true, updatable = false, length = 36)
    private String transferId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedTransfer() {
    }

    public ProcessedTransfer(String transferId, Instant processedAt) {
        this.transferId = transferId;
        this.processedAt = processedAt;
    }
}
