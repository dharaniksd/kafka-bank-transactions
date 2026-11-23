package com.example.bank.consumer.service;

import com.example.bank.consumer.domain.Account;
import com.example.bank.consumer.domain.AccountMovement;
import com.example.bank.consumer.domain.ProcessedTransfer;
import com.example.bank.consumer.repository.AccountMovementRepository;
import com.example.bank.consumer.repository.AccountRepository;
import com.example.bank.consumer.repository.ProcessedTransferRepository;
import com.example.bank.events.TransferRequested;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The debit, credit, audit rows, and inbox marker are one MySQL transaction.
 * Accounts are locked in id order to avoid deadlocks for opposing transfers.
 */
@Service
public class TransferProcessor {
    private final AccountRepository accountRepository;
    private final ProcessedTransferRepository processedTransferRepository;
    private final AccountMovementRepository accountMovementRepository;

    public TransferProcessor(AccountRepository accountRepository,
                             ProcessedTransferRepository processedTransferRepository,
                             AccountMovementRepository accountMovementRepository) {
        this.accountRepository = accountRepository;
        this.processedTransferRepository = processedTransferRepository;
        this.accountMovementRepository = accountMovementRepository;
    }

    @Transactional
    public void process(TransferRequested event) {
        validate(event);
        String transferId = event.transferId().toString();
        if (processedTransferRepository.existsByTransferId(transferId)) {
            return;
        }

        // Always obtain row locks in the same order, even if requests arrive in opposite directions.
        Long firstId = Math.min(event.debitAccountId(), event.creditAccountId());
        Long secondId = Math.max(event.debitAccountId(), event.creditAccountId());
        Account first = loadLocked(firstId);
        Account second = loadLocked(secondId);
        Account debit = event.debitAccountId().equals(firstId) ? first : second;
        Account credit = event.creditAccountId().equals(firstId) ? first : second;

        if (debit.getBalance().compareTo(event.amount()) < 0) {
            throw new InsufficientFundsException(debit.getId());
        }

        debit.debit(event.amount());
        credit.credit(event.amount());
        Instant processedAt = Instant.now();
        accountMovementRepository.save(new AccountMovement(
                transferId, debit.getId(), "DEBIT", event.amount(), processedAt));
        accountMovementRepository.save(new AccountMovement(
                transferId, credit.getId(), "CREDIT", event.amount(), processedAt));
        processedTransferRepository.save(new ProcessedTransfer(transferId, processedAt));
    }

    private Account loadLocked(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void validate(TransferRequested event) {
        if (event.transferId() == null || event.debitAccountId() == null || event.creditAccountId() == null
                || event.amount() == null || event.amount().signum() <= 0
                || event.debitAccountId().equals(event.creditAccountId())) {
            throw new IllegalArgumentException("Invalid transfer event");
        }
    }
}
