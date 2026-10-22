package com.banking.config;

import com.banking.dto.request.RegisterRequest;
import com.banking.entity.BankAccount;
import com.banking.entity.User;
import com.banking.entity.enums.AccountStatus;
import com.banking.entity.enums.Role;
import com.banking.repository.BankAccountRepository;
import com.banking.repository.UserRepository;
import com.banking.service.AuthService;
import com.banking.util.BankingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test") // Run in dev and mysql profiles, exclude during unit test runs
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            AuthService authService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial demo banking users and accounts...");

            // 1. Seed Alice Demo
            authService.register(new RegisterRequest(
                    "alice_demo",
                    "alice@minibanking.demo",
                    "Password123!",
                    "Alice",
                    "Smith"
            ));

            // 2. Seed Bob Demo
            authService.register(new RegisterRequest(
                    "bob_demo",
                    "bob@minibanking.demo",
                    "Password123!",
                    "Bob",
                    "Jones"
            ));

            // 3. Seed Admin Demo
            User admin = new User(
                    "admin_demo",
                    "admin@minibanking.demo",
                    passwordEncoder.encode("Admin123!"),
                    "System",
                    "Administrator",
                    Role.ROLE_ADMIN
            );
            admin = userRepository.save(admin);

            String adminAccNo;
            do {
                adminAccNo = BankingUtils.generateRandomAccountNumber();
            } while (bankAccountRepository.existsByAccountNumber(adminAccNo));

            BankAccount adminAccount = new BankAccount(
                    adminAccNo,
                    admin,
                    new BigDecimal("10000.00"),
                    "USD",
                    AccountStatus.ACTIVE
            );
            bankAccountRepository.save(adminAccount);

            log.info("Demo data initialized successfully:");
            log.info("  User Alice: alice_demo / Password123! ($1,000.00 simulated)");
            log.info("  User Bob:   bob_demo / Password123!   ($1,000.00 simulated)");
            log.info("  Admin:      admin_demo / Admin123!     ($10,000.00 simulated, ROLE_ADMIN)");
        }
    }
}
