package com.example.bank.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_movements")
public class AccountMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 6)
    private String direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountMovement() {
    }

    public AccountMovement(String transferId, Long accountId, String direction, BigDecimal amount, Instant createdAt) {
        this.transferId = transferId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}
