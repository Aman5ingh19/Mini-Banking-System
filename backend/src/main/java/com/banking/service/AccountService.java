package com.banking.service;

import com.banking.dto.response.AccountResponse;
import com.banking.dto.response.RecipientLookupResponse;
import com.banking.entity.BankAccount;
import com.banking.entity.User;
import com.banking.entity.enums.AccountStatus;
import com.banking.entity.enums.Role;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.UnauthorizedAccessException;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.UserRepository;
import com.banking.util.BankingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public AccountService(BankAccountRepository bankAccountRepository, UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + username));

        BankAccount account = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccountNotFoundException("Bank account not found for user: " + username));

        return mapToAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId, String username) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Bank account with ID " + accountId + " not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + username));

        // Enforce server-side ownership authorization
        boolean isOwner = account.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccessException("You are not authorized to view the balance of another user's account");
        }

        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public List<RecipientLookupResponse> searchRecipient(String query, String currentUsername) {
        String cleanQuery = query != null ? query.trim() : "";
        List<BankAccount> allAccounts = bankAccountRepository.findAll();

        return allAccounts.stream()
                // Filter out the current user's own account
                .filter(acc -> !acc.getUser().getUsername().equalsIgnoreCase(currentUsername))
                // Must be active
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
                // Match account number or username or full name
                .filter(acc -> cleanQuery.isEmpty() ||
                        acc.getAccountNumber().contains(cleanQuery) ||
                        acc.getUser().getUsername().toLowerCase().contains(cleanQuery.toLowerCase()) ||
                        (acc.getUser().getFirstName() + " " + acc.getUser().getLastName()).toLowerCase().contains(cleanQuery.toLowerCase()))
                .limit(10)
                .map(acc -> new RecipientLookupResponse(
                        acc.getAccountNumber(),
                        BankingUtils.maskAccountNumber(acc.getAccountNumber()),
                        acc.getUser().getFirstName() + " " + acc.getUser().getLastName() + " (@" + acc.getUser().getUsername() + ")",
                        acc.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    private AccountResponse mapToAccountResponse(BankAccount account) {
        User owner = account.getUser();
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                BankingUtils.maskAccountNumber(account.getAccountNumber()),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus().name(),
                owner.getUsername(),
                owner.getFirstName() + " " + owner.getLastName(),
                owner.getEmail(),
                account.getCreatedAt()
        );
    }
}
