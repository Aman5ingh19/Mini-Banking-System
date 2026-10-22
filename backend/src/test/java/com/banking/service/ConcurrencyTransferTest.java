package com.banking.service;

import com.banking.dto.request.RegisterRequest;
import com.banking.dto.request.TransferRequest;
import com.banking.dto.response.AuthResponse;
import com.banking.entity.BankAccount;
import com.banking.exception.InsufficientBalanceException;
import com.banking.repository.BankAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTransferTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AuthService authService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    @DisplayName("Concurrency: Two concurrent transfers against same balance prevent overdrawing via pessimistic locking")
    void testConcurrentTransfersAgainstSameBalance() throws InterruptedException, ExecutionException {
        // Setup sender with balance $100.00
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse sender = authService.register(new RegisterRequest(
                "concurrency_user_" + suffix,
                "concurrency_" + suffix + "@demo.com",
                "Password123!",
                "Concurrent",
                "Sender"
        ));

        // Adjust sender balance to exactly $100.00
        BankAccount senderAccount = bankAccountRepository.findById(sender.getAccountId()).get();
        senderAccount.setBalance(new BigDecimal("100.00"));
        bankAccountRepository.saveAndFlush(senderAccount);

        // Setup recipient
        String suffixR = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse recipient = authService.register(new RegisterRequest(
                "recip_" + suffixR,
                "recip_" + suffixR + "@demo.com",
                "Password123!",
                "Recipient",
                "User"
        ));

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Both threads attempt to transfer $80 simultaneously from the $100 balance
        Callable<Void> task1 = () -> {
            barrier.await();
            try {
                transferService.transferMoney(
                        sender.getUsername(),
                        new TransferRequest(recipient.getAccountNumber(), new BigDecimal("80.00"), "Transfer 1"),
                        UUID.randomUUID().toString()
                );
                successCount.incrementAndGet();
            } catch (InsufficientBalanceException ex) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            barrier.await();
            try {
                transferService.transferMoney(
                        sender.getUsername(),
                        new TransferRequest(recipient.getAccountNumber(), new BigDecimal("80.00"), "Transfer 2"),
                        UUID.randomUUID().toString()
                );
                successCount.incrementAndGet();
            } catch (InsufficientBalanceException ex) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        f1.get();
        f2.get();
        executor.shutdown();

        // Exactly one transfer should succeed and one should fail due to insufficient funds
        assertEquals(1, successCount.get(), "Exactly one transfer of $80 must succeed from $100 balance");
        assertEquals(1, failureCount.get(), "The second concurrent transfer must fail with InsufficientBalanceException");

        // The remaining balance must be strictly $20.00 (not negative / overdrawn)
        BankAccount finalSenderAccount = bankAccountRepository.findById(sender.getAccountId()).get();
        assertEquals(0, new BigDecimal("20.00").compareTo(finalSenderAccount.getBalance()),
                "Balance must be exactly $20.00 after one successful $80 debit from $100");
    }

    @Test
    @DisplayName("Deadlock Prevention: Simultaneous bidirectional transfers complete cleanly without deadlock")
    void testBidirectionalTransfersDeadlockAvoidance() throws InterruptedException, ExecutionException {
        String suffixA = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse userA = authService.register(new RegisterRequest(
                "deadlock_a_" + suffixA,
                "deadlock_a_" + suffixA + "@demo.com",
                "Password123!",
                "User",
                "A"
        ));

        String suffixB = UUID.randomUUID().toString().substring(0, 6);
        AuthResponse userB = authService.register(new RegisterRequest(
                "deadlock_b_" + suffixB,
                "deadlock_b_" + suffixB + "@demo.com",
                "Password123!",
                "User",
                "B"
        ));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        // A transfers $50 to B while B transfers $50 to A at the exact same instant
        Callable<Void> transferAtoB = () -> {
            barrier.await();
            transferService.transferMoney(
                    userA.getUsername(),
                    new TransferRequest(userB.getAccountNumber(), new BigDecimal("50.00"), "A to B"),
                    UUID.randomUUID().toString()
            );
            return null;
        };

        Callable<Void> transferBtoA = () -> {
            barrier.await();
            transferService.transferMoney(
                    userB.getUsername(),
                    new TransferRequest(userA.getAccountNumber(), new BigDecimal("50.00"), "B to A"),
                    UUID.randomUUID().toString()
            );
            return null;
        };

        Future<Void> f1 = executor.submit(transferAtoB);
        Future<Void> f2 = executor.submit(transferBtoA);

        // Both must complete cleanly without deadlock exceptions
        assertDoesNotThrow(() -> f1.get(10, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> f2.get(10, TimeUnit.SECONDS));

        executor.shutdown();
    }
}
