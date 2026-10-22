package com.banking.controller;

import com.banking.dto.request.TransferRequest;
import com.banking.dto.response.ErrorResponse;
import com.banking.dto.response.TransferReceiptResponse;
import com.banking.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Atomic money transfers with pessimistic locking and idempotency protection")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @Operation(summary = "Execute atomic money transfer",
               description = "Transfers simulated funds from the authenticated user's account to a recipient account with atomic database locking, deadlock ordering, and idempotency protection.")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
               description = "Unique client request ID (UUID) to prevent duplicate transfers on retries",
               schema = @Schema(type = "string", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer completed successfully",
                     content = @Content(schema = @Schema(implementation = TransferReceiptResponse.class))),
        @ApiResponse(responseCode = "400", description = "Insufficient funds, negative amount, or self-transfer attempt",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recipient account not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Concurrent in-flight duplicate transfer or idempotency collision",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransferReceiptResponse> executeTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransferReceiptResponse receipt = transferService.transferMoney(
                userDetails.getUsername(), request, idempotencyKey);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/{referenceId}")
    @Operation(summary = "Get transfer receipt by reference ID",
               description = "Fetches immutable transaction receipt and details for a given reference ID.")
    public ResponseEntity<TransferReceiptResponse> getTransferReceipt(
            @PathVariable String referenceId,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransferReceiptResponse receipt = transferService.getTransferByReferenceId(
                referenceId, userDetails.getUsername());
        return ResponseEntity.ok(receipt);
    }
}
