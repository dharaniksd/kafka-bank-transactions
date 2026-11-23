package com.example.bank.consumer.repository;

import com.example.bank.consumer.domain.AccountMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMovementRepository extends JpaRepository<AccountMovement, Long> {
}
