package com.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Transfer amount must be strictly greater than zero")
    @DecimalMin(value = "0.01", message = "Minimum transfer amount is $0.01")
    private BigDecimal amount;

    private String description;

    public TransferRequest() {
    }

    public TransferRequest(String recipientAccountNumber, BigDecimal amount, String description) {
        this.recipientAccountNumber = recipientAccountNumber;
        this.amount = amount;
        this.description = description;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
