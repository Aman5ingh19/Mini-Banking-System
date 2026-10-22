package com.banking.service;

import com.banking.dto.response.PaginatedResponse;
import com.banking.dto.response.TransactionResponse;
import com.banking.entity.BankAccount;
import com.banking.entity.LedgerEntry;
import com.banking.entity.Transfer;
import com.banking.entity.User;
import com.banking.entity.enums.LedgerEntryType;
import com.banking.exception.AccountNotFoundException;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.UserRepository;
import com.banking.util.BankingUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public TransactionService(
            LedgerEntryRepository ledgerEntryRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getTransactions(
            String username,
            String entryTypeStr,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + username));

        BankAccount account = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for user: " + username));

        LedgerEntryType entryType = null;
        if (entryTypeStr != null && !entryTypeStr.trim().isEmpty() && !entryTypeStr.equalsIgnoreCase("ALL")) {
            try {
                entryType = LedgerEntryType.valueOf(entryTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntry> entriesPage = ledgerEntryRepository.findEntriesForAccount(
                account.getId(), entryType, startDateTime, endDateTime, pageable);

        List<TransactionResponse> items = entriesPage.getContent().stream()
                .map(entry -> mapToTransactionResponse(entry, account))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                items,
                entriesPage.getNumber(),
                entriesPage.getSize(),
                entriesPage.getTotalElements(),
                entriesPage.getTotalPages(),
                entriesPage.isFirst(),
                entriesPage.isLast()
        );
    }

    private TransactionResponse mapToTransactionResponse(LedgerEntry entry, BankAccount currentAccount) {
        Transfer transfer = entry.getTransfer();

        String counterpartyAccount = "SYSTEM";
        String counterpartyMasked = "SYSTEM RESERVE";
        String counterpartyName = "System Treasury";

        if (entry.getEntryType() == LedgerEntryType.DEBIT) {
            // Funds sent OUT -> Counterparty is the Destination
            BankAccount dest = transfer.getDestinationAccount();
            if (dest != null) {
                counterpartyAccount = dest.getAccountNumber();
                counterpartyMasked = BankingUtils.maskAccountNumber(dest.getAccountNumber());
                counterpartyName = dest.getUser().getFirstName() + " " + dest.getUser().getLastName();
            }
        } else {
            // Funds received IN -> Counterparty is the Source
            BankAccount src = transfer.getSourceAccount();
            if (src != null) {
                counterpartyAccount = src.getAccountNumber();
                counterpartyMasked = BankingUtils.maskAccountNumber(src.getAccountNumber());
                counterpartyName = src.getUser().getFirstName() + " " + src.getUser().getLastName();
            } else {
                counterpartyMasked = "System Welcome Credit";
                counterpartyName = "Demo Bank Treasury";
            }
        }

        return new TransactionResponse(
                entry.getId(),
                transfer.getReferenceId(),
                entry.getEntryType().name(),
                entry.getAmount(),
                entry.getResultingBalance(),
                currentAccount.getCurrency(),
                transfer.getType().name(),
                transfer.getStatus().name(),
                transfer.getDescription(),
                counterpartyAccount,
                counterpartyMasked,
                counterpartyName,
                entry.getCreatedAt()
        );
    }
}
