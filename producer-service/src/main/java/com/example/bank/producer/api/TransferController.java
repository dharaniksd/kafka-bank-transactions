package com.example.bank.producer.api;

import com.example.bank.producer.service.TransferPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferPublisher transferPublisher;

    public TransferController(TransferPublisher transferPublisher) {
        this.transferPublisher = transferPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransferAccepted create(@Valid @RequestBody TransferRequest request) {
        return transferPublisher.publish(request);
    }
}
