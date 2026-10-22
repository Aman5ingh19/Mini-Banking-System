package com.banking.service;

import com.banking.dto.request.DepositWithdrawalRequest;
import com.banking.dto.request.TransferRequest;
import com.banking.dto.response.TransferReceiptResponse;
import com.banking.entity.BankAccount;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Transfer;
import com.banking.entity.User;
import com.banking.entity.enums.AccountStatus;
import com.banking.entity.enums.LedgerEntryType;
import com.banking.entity.enums.Role;
import com.banking.entity.enums.TransferStatus;
import com.banking.entity.enums.TransferType;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransferException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.TransferRepository;
import com.banking.repository.UserRepository;
import com.banking.util.BankingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final BankAccountRepository bankAccountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;

    public TransferService(
            BankAccountRepository bankAccountRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            UserRepository userRepository,
            IdempotencyService idempotencyService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Executes atomic money transfer between demo accounts with pessimistic locking,
     * deadlock avoidance via deterministic lock ordering, double-entry ledger bookkeeping,
     * and request idempotency protection.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransferReceiptResponse transferMoney(String senderUsername, TransferRequest request, String idempotencyKey) {
        // 1. Identify Authenticated Sender User
        User senderUser = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new AccountNotFoundException("Sender user not found: " + senderUsername));

        // 2. Validate Idempotency Guard (detect duplicate retries)
        String payloadHash = idempotencyService.computePayloadHash(
                request.getRecipientAccountNumber() + ":" + request.getAmount().toPlainString() + ":" + request.getDescription()
        );
        TransferReceiptResponse cachedResponse = idempotencyService.getOrLockKey(senderUser, idempotencyKey, payloadHash);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            // 3. Preliminary Lookups of Account IDs only
            Long senderAccountId = bankAccountRepository.findIdByUserId(senderUser.getId())
                    .orElseThrow(() -> new AccountNotFoundException("No bank account found for sender: " + senderUsername));

            Long recipientAccountId = bankAccountRepository.findIdByAccountNumber(request.getRecipientAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Recipient account number '" + request.getRecipientAccountNumber() + "' not found"));

            // 4. Validate Business Rules
            if (senderAccountId.equals(recipientAccountId)) {
                throw new InvalidTransferException("Cannot transfer money to the same bank account");
            }

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransferException("Transfer amount must be strictly greater than zero");
            }

            // 5. Deadlock Prevention via Deterministic Lock Ordering:
            // Always lock the account with the smaller numerical ID first, then larger ID.
            Long firstLockId = Math.min(senderAccountId, recipientAccountId);
            Long secondLockId = Math.max(senderAccountId, recipientAccountId);

            BankAccount firstLocked = bankAccountRepository.findByIdForUpdate(firstLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account lock acquisition failed for ID: " + firstLockId));
            BankAccount secondLocked = bankAccountRepository.findByIdForUpdate(secondLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account lock acquisition failed for ID: " + secondLockId));

            BankAccount lockedSender = senderAccountId.equals(firstLockId) ? firstLocked : secondLocked;
            BankAccount lockedRecipient = recipientAccountId.equals(firstLockId) ? firstLocked : secondLocked;

            if (lockedSender.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransferException("Sender account is " + lockedSender.getStatus() + ". Transfers are blocked.");
            }

            if (lockedRecipient.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransferException("Recipient account is " + lockedRecipient.getStatus() + ". Cannot receive funds.");
            }

            // 6. Verify Sufficient Balance Inside Protected Lock
            if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
                log.warn("Transfer failed: Insufficient balance for user '{}'. Available: ${}, Requested: ${}",
                        senderUsername, lockedSender.getBalance(), request.getAmount());
                recordFailedTransfer(lockedSender, lockedRecipient, request.getAmount(), "Insufficient available balance");
                idempotencyService.recordFailure(senderUser, idempotencyKey);
                throw new InsufficientBalanceException("Insufficient available balance. Current balance is $" +
                        lockedSender.getBalance() + ", attempted transfer of $" + request.getAmount());
            }

            // 7. Atomic Balance Updates
            lockedSender.debit(request.getAmount());
            lockedRecipient.credit(request.getAmount());
            bankAccountRepository.save(lockedSender);
            bankAccountRepository.save(lockedRecipient);

            // 8. Generate Unique Reference ID & Record Transfer
            String referenceId = "TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            Transfer transfer = new Transfer(
                    referenceId,
                    lockedSender,
                    lockedRecipient,
                    request.getAmount(),
                    TransferType.TRANSFER,
                    TransferStatus.SUCCESS,
                    request.getDescription() != null ? request.getDescription() : "Transfer to " + BankingUtils.maskAccountNumber(lockedRecipient.getAccountNumber())
            );
            transfer = transferRepository.save(transfer);

            // 9. Write Immutable Double-Entry Bookkeeping Ledger Entries
            LedgerEntry debitEntry = new LedgerEntry(
                    transfer,
                    lockedSender,
                    LedgerEntryType.DEBIT,
                    request.getAmount(),
                    lockedSender.getBalance()
            );
            ledgerEntryRepository.save(debitEntry);

            LedgerEntry creditEntry = new LedgerEntry(
                    transfer,
                    lockedRecipient,
                    LedgerEntryType.CREDIT,
                    request.getAmount(),
                    lockedRecipient.getBalance()
            );
            ledgerEntryRepository.save(creditEntry);

            log.info("Atomic transfer SUCCESS: Ref='{}', ${} from '{}' to '{}'",
                    referenceId, request.getAmount(),
                    BankingUtils.maskAccountNumber(lockedSender.getAccountNumber()),
                    BankingUtils.maskAccountNumber(lockedRecipient.getAccountNumber()));

            // 10. Assemble Receipt Response
            TransferReceiptResponse receipt = new TransferReceiptResponse(
                    referenceId,
                    lockedSender.getAccountNumber(),
                    BankingUtils.maskAccountNumber(lockedSender.getAccountNumber()),
                    lockedRecipient.getAccountNumber(),
                    BankingUtils.maskAccountNumber(lockedRecipient.getAccountNumber()),
                    lockedSender.getUser().getFirstName() + " " + lockedSender.getUser().getLastName(),
                    lockedRecipient.getUser().getFirstName() + " " + lockedRecipient.getUser().getLastName(),
                    request.getAmount(),
                    lockedSender.getCurrency(),
                    TransferType.TRANSFER.name(),
                    TransferStatus.SUCCESS.name(),
                    transfer.getDescription(),
                    lockedSender.getBalance(),
                    transfer.getCreatedAt()
            );

            // 11. Finalize Idempotency Record
            idempotencyService.recordSuccess(senderUser, idempotencyKey, receipt);

            return receipt;

        } catch (Exception ex) {
            idempotencyService.recordFailure(senderUser, idempotencyKey);
            throw ex;
        }
    }

    /**
     * Retrieve transfer receipt by reference ID with server-side ownership authorization.
     */
    @Transactional(readOnly = true)
    public TransferReceiptResponse getTransferByReferenceId(String referenceId, String currentUsername) {
        Transfer transfer = transferRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new AccountNotFoundException("Transfer with reference ID '" + referenceId + "' not found"));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + currentUsername));

        boolean isSender = transfer.getSourceAccount() != null &&
                transfer.getSourceAccount().getUser().getId().equals(currentUser.getId());
        boolean isRecipient = transfer.getDestinationAccount() != null &&
                transfer.getDestinationAccount().getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isSender && !isRecipient && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to view this transaction receipt");
        }

        String sourceAcc = transfer.getSourceAccount() != null ? transfer.getSourceAccount().getAccountNumber() : "SYSTEM";
        String senderName = transfer.getSourceAccount() != null ?
                transfer.getSourceAccount().getUser().getFirstName() + " " + transfer.getSourceAccount().getUser().getLastName() : "System Demo Reserve";

        return new TransferReceiptResponse(
                transfer.getReferenceId(),
                sourceAcc,
                BankingUtils.maskAccountNumber(sourceAcc),
                transfer.getDestinationAccount().getAccountNumber(),
                BankingUtils.maskAccountNumber(transfer.getDestinationAccount().getAccountNumber()),
                senderName,
                transfer.getDestinationAccount().getUser().getFirstName() + " " + transfer.getDestinationAccount().getUser().getLastName(),
                transfer.getAmount(),
                transfer.getDestinationAccount().getCurrency(),
                transfer.getType().name(),
                transfer.getStatus().name(),
                transfer.getDescription(),
                isSender && transfer.getSourceAccount() != null ? transfer.getSourceAccount().getBalance() : transfer.getDestinationAccount().getBalance(),
                transfer.getCreatedAt()
        );
    }

    /**
     * Administrative demo deposit: Authorized admin adds simulated funds to an account.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferReceiptResponse adminDeposit(Long accountId, DepositWithdrawalRequest request) {
        BankAccount account = bankAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found"));

        account.credit(request.getAmount());
        bankAccountRepository.save(account);

        String referenceId = "DEP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        Transfer deposit = new Transfer(
                referenceId,
                null,
                account,
                request.getAmount(),
                TransferType.DEPOSIT,
                TransferStatus.SUCCESS,
                request.getDescription() != null ? request.getDescription() : "Admin Demo Deposit"
        );
        deposit = transferRepository.save(deposit);

        LedgerEntry entry = new LedgerEntry(
                deposit,
                account,
                LedgerEntryType.CREDIT,
                request.getAmount(),
                account.getBalance()
        );
        ledgerEntryRepository.save(entry);

        return new TransferReceiptResponse(
                referenceId,
                "SYSTEM",
                "SYSTEM RESERVE",
                account.getAccountNumber(),
                BankingUtils.maskAccountNumber(account.getAccountNumber()),
                "System Treasury",
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                request.getAmount(),
                account.getCurrency(),
                TransferType.DEPOSIT.name(),
                TransferStatus.SUCCESS.name(),
                deposit.getDescription(),
                account.getBalance(),
                deposit.getCreatedAt()
        );
    }

    /**
     * Administrative demo withdrawal: Authorized admin deducts simulated funds.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferReceiptResponse adminWithdrawal(Long accountId, DepositWithdrawalRequest request) {
        BankAccount account = bankAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found"));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Account balance ($" + account.getBalance() +
                    ") is insufficient for withdrawal of $" + request.getAmount());
        }

        account.debit(request.getAmount());
        bankAccountRepository.save(account);

        String referenceId = "WTH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        Transfer withdrawal = new Transfer(
                referenceId,
                account,
                account,
                request.getAmount(),
                TransferType.WITHDRAWAL,
                TransferStatus.SUCCESS,
                request.getDescription() != null ? request.getDescription() : "Admin Demo Withdrawal"
        );
        withdrawal = transferRepository.save(withdrawal);

        LedgerEntry entry = new LedgerEntry(
                withdrawal,
                account,
                LedgerEntryType.DEBIT,
                request.getAmount(),
                account.getBalance()
        );
        ledgerEntryRepository.save(entry);

        return new TransferReceiptResponse(
                referenceId,
                account.getAccountNumber(),
                BankingUtils.maskAccountNumber(account.getAccountNumber()),
                "SYSTEM",
                "SYSTEM RESERVE",
                account.getUser().getFirstName() + " " + account.getUser().getLastName(),
                "System Treasury",
                request.getAmount(),
                account.getCurrency(),
                TransferType.WITHDRAWAL.name(),
                TransferStatus.SUCCESS.name(),
                withdrawal.getDescription(),
                account.getBalance(),
                withdrawal.getCreatedAt()
        );
    }

    private void recordFailedTransfer(BankAccount sender, BankAccount recipient, BigDecimal amount, String reason) {
        try {
            Transfer failedTransfer = new Transfer(
                    "FAILED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    sender,
                    recipient,
                    amount,
                    TransferType.TRANSFER,
                    TransferStatus.FAILED,
                    reason
            );
            failedTransfer.setFailureReason(reason);
            transferRepository.save(failedTransfer);
        } catch (Exception e) {
            log.error("Failed to record failed transfer attempt", e);
        }
    }
}
