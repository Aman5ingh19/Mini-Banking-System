package com.banking.service;

import com.banking.dto.request.RegisterRequest;
import com.banking.dto.request.TransferRequest;
import com.banking.dto.response.AuthResponse;
import com.banking.dto.response.TransferReceiptResponse;
import com.banking.entity.BankAccount;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Transfer;
import com.banking.entity.enums.LedgerEntryType;
import com.banking.entity.enums.TransferStatus;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.DuplicateIdempotencyKeyException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransferException;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AuthService authService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private AuthResponse senderAuth;
    private AuthResponse recipientAuth;

    @BeforeEach
    void setUp() {
        // Register two distinct demo users for clean testing
        String suffix1 = UUID.randomUUID().toString().substring(0, 6);
        RegisterRequest req1 = new RegisterRequest(
                "alice_" + suffix1,
                "alice_" + suffix1 + "@demo.com",
                "Password123!",
                "Alice",
                "Smith"
        );
        senderAuth = authService.register(req1);

        String suffix2 = UUID.randomUUID().toString().substring(0, 6);
        RegisterRequest req2 = new RegisterRequest(
                "bob_" + suffix2,
                "bob_" + suffix2 + "@demo.com",
                "Password123!",
                "Bob",
                "Jones"
        );
        recipientAuth = authService.register(req2);
    }

    @Test
    @DisplayName("Successful Transfer: Debits sender, credits receiver, preserves total balance, and creates matching ledger entries")
    void testSuccessfulTransfer() {
        BigDecimal transferAmount = new BigDecimal("250.00");
        String idempotencyKey = UUID.randomUUID().toString();

        BigDecimal initialSenderBalance = bankAccountRepository.findById(senderAuth.getAccountId()).get().getBalance();
        BigDecimal initialRecipientBalance = bankAccountRepository.findById(recipientAuth.getAccountId()).get().getBalance();

        TransferRequest request = new TransferRequest(
                recipientAuth.getAccountNumber(),
                transferAmount,
                "Payment for services"
        );

        TransferReceiptResponse receipt = transferService.transferMoney(
                senderAuth.getUsername(), request, idempotencyKey);

        assertNotNull(receipt);
        assertEquals(TransferStatus.SUCCESS.name(), receipt.getStatus());
        assertEquals(0, transferAmount.compareTo(receipt.getAmount()));
        assertNotNull(receipt.getReferenceId());

        // Verify Sender balance
        BankAccount updatedSender = bankAccountRepository.findById(senderAuth.getAccountId()).get();
        assertEquals(0, initialSenderBalance.subtract(transferAmount).compareTo(updatedSender.getBalance()));

        // Verify Recipient balance
        BankAccount updatedRecipient = bankAccountRepository.findById(recipientAuth.getAccountId()).get();
        assertEquals(0, initialRecipientBalance.add(transferAmount).compareTo(updatedRecipient.getBalance()));

        // Total sum invariant check (no money was created or destroyed)
        BigDecimal totalInitial = initialSenderBalance.add(initialRecipientBalance);
        BigDecimal totalAfter = updatedSender.getBalance().add(updatedRecipient.getBalance());
        assertEquals(0, totalInitial.compareTo(totalAfter));

        // Verify Double-Entry Ledger Bookkeeping
        Transfer transfer = transferRepository.findByReferenceId(receipt.getReferenceId()).orElseThrow();
        assertEquals(0, transferAmount.compareTo(transfer.getAmount()));

        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(e -> e.getTransfer().getId().equals(transfer.getId()))
                .toList();

        assertEquals(2, entries.size(), "Each transfer must produce exactly two ledger entries (DEBIT and CREDIT)");

        LedgerEntry debitEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .findFirst()
                .orElseThrow();
        assertEquals(updatedSender.getId(), debitEntry.getAccount().getId());
        assertEquals(0, updatedSender.getBalance().compareTo(debitEntry.getResultingBalance()));

        LedgerEntry creditEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.CREDIT)
                .findFirst()
                .orElseThrow();
        assertEquals(updatedRecipient.getId(), creditEntry.getAccount().getId());
        assertEquals(0, updatedRecipient.getBalance().compareTo(creditEntry.getResultingBalance()));
    }

    @Test
    @DisplayName("Insufficient Balance: Rejects transfer with rollback, leaves balances unchanged")
    void testInsufficientBalanceRollback() {
        BigDecimal senderBalance = bankAccountRepository.findById(senderAuth.getAccountId()).get().getBalance();
        BigDecimal excessiveAmount = senderBalance.add(new BigDecimal("500.00"));

        TransferRequest request = new TransferRequest(
                recipientAuth.getAccountNumber(),
                excessiveAmount,
                "Overdrawn transfer"
        );

        assertThrows(InsufficientBalanceException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), request, UUID.randomUUID().toString())
        );

        // Verify balance remained intact
        BankAccount senderAfter = bankAccountRepository.findById(senderAuth.getAccountId()).get();
        assertEquals(0, senderBalance.compareTo(senderAfter.getBalance()));

        BankAccount recipientAfter = bankAccountRepository.findById(recipientAuth.getAccountId()).get();
        assertEquals(0, recipientAuth.getBalance().compareTo(recipientAfter.getBalance()));
    }

    @Test
    @DisplayName("Invalid Transfer: Cannot transfer to own account")
    void testSelfTransferRejection() {
        TransferRequest request = new TransferRequest(
                senderAuth.getAccountNumber(), // Transfer to self!
                new BigDecimal("50.00"),
                "Self transfer"
        );

        assertThrows(InvalidTransferException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), request, UUID.randomUUID().toString())
        );
    }

    @Test
    @DisplayName("Invalid Transfer: Negative or zero amount rejected")
    void testNegativeAmountRejection() {
        TransferRequest reqZero = new TransferRequest(recipientAuth.getAccountNumber(), BigDecimal.ZERO, "Zero");
        assertThrows(InvalidTransferException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), reqZero, UUID.randomUUID().toString())
        );

        TransferRequest reqNegative = new TransferRequest(recipientAuth.getAccountNumber(), new BigDecimal("-10.00"), "Neg");
        assertThrows(InvalidTransferException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), reqNegative, UUID.randomUUID().toString())
        );
    }

    @Test
    @DisplayName("Unknown Recipient: Rejects transfer with AccountNotFoundException")
    void testUnknownRecipient() {
        TransferRequest request = new TransferRequest(
                "999999999999",
                new BigDecimal("50.00"),
                "To unknown"
        );

        assertThrows(AccountNotFoundException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), request, UUID.randomUUID().toString())
        );
    }

    @Test
    @DisplayName("Idempotency: Repeated key returns cached response without double-debiting")
    void testIdempotencyReplay() {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(
                recipientAuth.getAccountNumber(),
                new BigDecimal("100.00"),
                "Payment"
        );

        // First transfer attempt
        TransferReceiptResponse firstReceipt = transferService.transferMoney(
                senderAuth.getUsername(), request, idempotencyKey);
        assertNotNull(firstReceipt);

        BigDecimal senderBalanceAfterFirst = bankAccountRepository.findById(senderAuth.getAccountId()).get().getBalance();

        // Second transfer attempt with IDENTICAL idempotency key and payload
        TransferReceiptResponse secondReceipt = transferService.transferMoney(
                senderAuth.getUsername(), request, idempotencyKey);

        assertEquals(firstReceipt.getReferenceId(), secondReceipt.getReferenceId(),
                "Repeated idempotency key must return the exact same transaction reference ID");

        // Verify sender was NOT debited a second time
        BigDecimal senderBalanceAfterSecond = bankAccountRepository.findById(senderAuth.getAccountId()).get().getBalance();
        assertEquals(0, senderBalanceAfterFirst.compareTo(senderBalanceAfterSecond),
                "Sender balance must remain unchanged after idempotent request replay");
    }

    @Test
    @DisplayName("Idempotency Conflict: Same key reused with different payload is rejected")
    void testIdempotencyKeyPayloadMismatch() {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest originalReq = new TransferRequest(
                recipientAuth.getAccountNumber(),
                new BigDecimal("100.00"),
                "Original transfer"
        );
        transferService.transferMoney(senderAuth.getUsername(), originalReq, idempotencyKey);

        // Same idempotency key, but different amount!
        TransferRequest tamperedReq = new TransferRequest(
                recipientAuth.getAccountNumber(),
                new BigDecimal("500.00"),
                "Different transfer"
        );

        assertThrows(DuplicateIdempotencyKeyException.class, () ->
                transferService.transferMoney(senderAuth.getUsername(), tamperedReq, idempotencyKey)
        );
    }
}
