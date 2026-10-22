package com.banking.controller;

import com.banking.dto.response.PaginatedResponse;
import com.banking.dto.response.TransactionResponse;
import com.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Query transaction ledger history with filtering and pagination")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Get current user's paginated transaction history",
               description = "Returns immutable double-entry ledger records for the authenticated user with optional date range and entry type (DEBIT/CREDIT) filters.")
    public ResponseEntity<PaginatedResponse<TransactionResponse>> getTransactions(
            @Parameter(description = "Filter by entry type: DEBIT, CREDIT, or ALL")
            @RequestParam(required = false, defaultValue = "ALL") String type,

            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Zero-indexed page number")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "Number of records per page")
            @RequestParam(required = false, defaultValue = "10") int size,

            @AuthenticationPrincipal UserDetails userDetails) {

        PaginatedResponse<TransactionResponse> response = transactionService.getTransactions(
                userDetails.getUsername(), type, startDate, endDate, page, size);
        return ResponseEntity.ok(response);
    }
}
