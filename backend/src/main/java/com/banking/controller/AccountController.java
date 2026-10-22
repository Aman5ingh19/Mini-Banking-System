package com.banking.controller;

import com.banking.dto.response.AccountResponse;
import com.banking.dto.response.RecipientLookupResponse;
import com.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Bank account details, balance inspection, and safe recipient lookup")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's demo bank account details",
               description = "Returns the authenticated user's account details, available balance, and owner profile.")
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = accountService.getMyAccount(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance with strict ownership check",
               description = "Verifies server-side that the requester owns the target account or possesses ADMIN role before returning the balance.")
    public ResponseEntity<Map<String, Object>> getAccountBalance(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        BigDecimal balance = accountService.getAccountBalance(accountId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance,
                "currency", "USD"
        ));
    }

    @GetMapping("/search")
    @Operation(summary = "Search recipients for transfer",
               description = "Allows finding recipient accounts without exposing confidential information like balances or private details.")
    public ResponseEntity<List<RecipientLookupResponse>> searchRecipients(
            @RequestParam(required = false, defaultValue = "") String query,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<RecipientLookupResponse> results = accountService.searchRecipient(query, userDetails.getUsername());
        return ResponseEntity.ok(results);
    }
}
