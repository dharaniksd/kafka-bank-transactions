package com.example.bank.consumer.repository;

import com.example.bank.consumer.domain.ProcessedTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedTransferRepository extends JpaRepository<ProcessedTransfer, Long> {
    boolean existsByTransferId(String transferId);
}
