package com.example.bank.consumer.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountId) {
        super("Account " + accountId + " does not exist");
    }
}
