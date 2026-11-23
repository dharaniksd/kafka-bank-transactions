package com.example.bank.producer.service;

import com.example.bank.events.TransferRequested;
import com.example.bank.events.avro.TransferRequestedAvro;
import com.example.bank.producer.api.TransferAccepted;
import com.example.bank.producer.api.TransferRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Publishes each transfer inside a Kafka transaction. */
@Service
public class TransferPublisher {
    private final KafkaTemplate<String, TransferRequestedAvro> kafkaTemplate;
    private final String transferTopic;

    public TransferPublisher(KafkaTemplate<String, TransferRequestedAvro> kafkaTemplate,
                             @Value("${app.kafka.transfer-topic}") String transferTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.transferTopic = transferTopic;
    }

    public TransferAccepted publish(TransferRequest request) {
        if (request.debitAccountId().equals(request.creditAccountId())) {
            throw new IllegalArgumentException("Debit and credit accounts must be different");
        }

        UUID transferId = UUID.randomUUID();
        TransferRequested event = new TransferRequested(
                transferId,
                request.debitAccountId(),
                request.creditAccountId(),
                request.amount(),
                Instant.now());
        TransferRequestedAvro avroEvent = new TransferRequestedAvro(
                event.transferId().toString(),
                event.debitAccountId(),
                event.creditAccountId(),
                event.amount(),
                event.requestedAt().toEpochMilli());

        // The Avro schema is registered before the record is committed. The record is
        // visible to read_committed consumers only after commitTransaction succeeds.
        kafkaTemplate.executeInTransaction(operations -> {
            try {
                SendResult<String, TransferRequestedAvro> ignored = operations
                        .send(transferTopic, event.debitAccountId().toString(), avroEvent)
                        .get(15, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new TransferPublishException("Could not commit transfer to Kafka", exception);
            }
            return true;
        });

        return new TransferAccepted(transferId, "PUBLISHED");
    }
}
