package com.example.bank.consumer.service;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId) {
        super("Account " + accountId + " has insufficient funds");
    }
}
