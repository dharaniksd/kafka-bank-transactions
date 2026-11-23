package com.example.bank.consumer.api;

import com.example.bank.consumer.domain.Account;
import com.example.bank.consumer.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accountRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getBalance());
    }
}
