package com.example.bank.consumer.service;

import com.example.bank.consumer.domain.Account;
import com.example.bank.consumer.repository.AccountMovementRepository;
import com.example.bank.consumer.repository.AccountRepository;
import com.example.bank.consumer.repository.ProcessedTransferRepository;
import com.example.bank.events.TransferRequested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferProcessorTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ProcessedTransferRepository processedTransferRepository;
    @Mock
    private AccountMovementRepository accountMovementRepository;
    @InjectMocks
    private TransferProcessor transferProcessor;

    @Test
    void debitsAndCreditsTogetherForANewTransfer() {
        Account debit = new Account("ACC-1001", new BigDecimal("100.00"));
        Account credit = new Account("ACC-1002", new BigDecimal("20.00"));
        TransferRequested event = event(new BigDecimal("35.00"));
        when(processedTransferRepository.existsByTransferId(event.transferId().toString())).thenReturn(false);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(debit));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(credit));

        transferProcessor.process(event);

        assertThat(debit.getBalance()).isEqualByComparingTo("65.00");
        assertThat(credit.getBalance()).isEqualByComparingTo("55.00");
        verify(accountMovementRepository, times(2)).save(any());
        verify(processedTransferRepository).save(any());
    }

    @Test
    void doesNotChangeBalancesWhenFundsAreInsufficient() {
        Account debit = new Account("ACC-1001", new BigDecimal("10.00"));
        Account credit = new Account("ACC-1002", new BigDecimal("20.00"));
        TransferRequested event = event(new BigDecimal("35.00"));
        when(processedTransferRepository.existsByTransferId(event.transferId().toString())).thenReturn(false);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(debit));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(credit));

        assertThatThrownBy(() -> transferProcessor.process(event))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(debit.getBalance()).isEqualByComparingTo("10.00");
        assertThat(credit.getBalance()).isEqualByComparingTo("20.00");
        verify(accountMovementRepository, never()).save(any());
        verify(processedTransferRepository, never()).save(any());
    }

    @Test
    void ignoresAnAlreadyProcessedTransfer() {
        TransferRequested event = event(new BigDecimal("35.00"));
        when(processedTransferRepository.existsByTransferId(event.transferId().toString())).thenReturn(true);

        transferProcessor.process(event);

        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(accountMovementRepository, never()).save(any());
    }

    private TransferRequested event(BigDecimal amount) {
        return new TransferRequested(UUID.randomUUID(), 1L, 2L, amount, Instant.now());
    }
}
