package com.banking.controller;

import com.banking.dto.request.DepositWithdrawalRequest;
import com.banking.dto.response.TransferReceiptResponse;
import com.banking.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Privileged demo operations restricted strictly to ROLE_ADMIN")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TransferService transferService;

    public AdminController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/accounts/{accountId}/deposits")
    @Operation(summary = "Authorized demo deposit (Admin only)",
               description = "Credits simulated demo funds into an account. Regular users are strictly forbidden from calling this endpoint.")
    public ResponseEntity<TransferReceiptResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositWithdrawalRequest request) {

        TransferReceiptResponse response = transferService.adminDeposit(accountId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/{accountId}/withdrawals")
    @Operation(summary = "Authorized demo withdrawal (Admin only)",
               description = "Debits simulated demo funds from an account. Protected by role-based authorization.")
    public ResponseEntity<TransferReceiptResponse> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositWithdrawalRequest request) {

        TransferReceiptResponse response = transferService.adminWithdrawal(accountId, request);
        return ResponseEntity.ok(response);
    }
}
