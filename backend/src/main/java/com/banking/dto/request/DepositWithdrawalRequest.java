package com.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class DepositWithdrawalRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly greater than zero")
    @DecimalMin(value = "0.01", message = "Minimum transaction amount is $0.01")
    private BigDecimal amount;

    private String description;

    public DepositWithdrawalRequest() {
    }

    public DepositWithdrawalRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
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
