package com.example.bank.producer.api;

import java.util.UUID;

public record TransferAccepted(UUID transferId, String status) {
}
