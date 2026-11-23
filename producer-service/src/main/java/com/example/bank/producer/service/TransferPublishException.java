package com.example.bank.producer.service;

public class TransferPublishException extends RuntimeException {
    public TransferPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
