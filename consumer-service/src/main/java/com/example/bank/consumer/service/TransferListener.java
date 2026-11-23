package com.example.bank.consumer.service;

import com.example.bank.events.TransferRequested;
import com.example.bank.events.avro.TransferRequestedAvro;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TransferListener {
    private final TransferProcessor transferProcessor;

    public TransferListener(TransferProcessor transferProcessor) {
        this.transferProcessor = transferProcessor;
    }

    /**
     * Offset commits occur only after this method returns. If MySQL rolls back,
     * Kafka redelivers the record; processed_transfers makes that safe.
     */
    @KafkaListener(topics = "${app.kafka.transfer-topic}", groupId = "${app.kafka.consumer-group}")
    public void onTransfer(TransferRequestedAvro event) {
        transferProcessor.process(new TransferRequested(
                UUID.fromString(event.getTransferId()),
                event.getDebitAccountId(),
                event.getCreditAccountId(),
                event.getAmount(),
                Instant.ofEpochMilli(event.getRequestedAtEpochMillis())));
    }
}
