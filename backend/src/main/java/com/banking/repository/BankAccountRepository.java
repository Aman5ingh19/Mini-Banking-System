package com.banking.repository;

import com.banking.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByUserId(Long userId);

    Optional<BankAccount> findByUserUsername(String username);

    boolean existsByAccountNumber(String accountNumber);

    List<BankAccount> findAllByUserId(Long userId);

    @Query("SELECT b.id FROM BankAccount b WHERE b.user.id = :userId")
    Optional<Long> findIdByUserId(@Param("userId") Long userId);

    @Query("SELECT b.id FROM BankAccount b WHERE b.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * Pessimistic Write Lock by ID (translates to SELECT ... FOR UPDATE).
     * Used in TransferService to lock accounts in deterministic order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);

    /**
     * Pessimistic Write Lock by Account Number (translates to SELECT ... FOR UPDATE).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
