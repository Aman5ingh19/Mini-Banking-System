package com.banking.service;

import com.banking.dto.request.LoginRequest;
import com.banking.dto.request.RegisterRequest;
import com.banking.dto.response.AuthResponse;
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
import com.banking.exception.UserAlreadyExistsException;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.LedgerEntryRepository;
import com.banking.repository.TransferRepository;
import com.banking.repository.UserRepository;
import com.banking.security.JwtTokenProvider;
import com.banking.util.BankingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final BigDecimal initialDemoBalance;

    public AuthService(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            @Value("${app.banking.initial-demo-balance:1000.00}") BigDecimal initialDemoBalance) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.initialDemoBalance = initialDemoBalance;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        // 1. Create and persist User
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                Role.ROLE_USER
        );
        user = userRepository.save(user);

        // 2. Generate unique 12-digit account number
        String accountNumber;
        do {
            accountNumber = BankingUtils.generateRandomAccountNumber();
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));

        // 3. Create demo BankAccount with initial simulated balance
        BankAccount account = new BankAccount(
                accountNumber,
                user,
                initialDemoBalance,
                "USD",
                AccountStatus.ACTIVE
        );
        account = bankAccountRepository.save(account);

        // 4. Record Initial Simulated Deposit Ledger Entry for audit trail
        if (initialDemoBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transfer initialDeposit = new Transfer(
                    "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    null,
                    account,
                    initialDemoBalance,
                    TransferType.DEPOSIT,
                    TransferStatus.SUCCESS,
                    "Simulated Demo Welcome Credit"
            );
            initialDeposit = transferRepository.save(initialDeposit);

            LedgerEntry ledgerEntry = new LedgerEntry(
                    initialDeposit,
                    account,
                    LedgerEntryType.CREDIT,
                    initialDemoBalance,
                    initialDemoBalance
            );
            ledgerEntryRepository.save(ledgerEntry);
        }

        log.info("Registered user '{}' with account '{}' and simulated balance ${}",
                user.getUsername(), BankingUtils.maskAccountNumber(account.getAccountNumber()), account.getBalance());

        // 5. Generate JWT token
        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                account.getId(),
                account.getAccountNumber(),
                BankingUtils.maskAccountNumber(account.getAccountNumber()),
                account.getBalance(),
                account.getCurrency()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate with Spring Security manager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException("User not found with username: " + username));

        BankAccount account = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccountNotFoundException("No active bank account associated with user: " + username));

        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                account.getId(),
                account.getAccountNumber(),
                BankingUtils.maskAccountNumber(account.getAccountNumber()),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
