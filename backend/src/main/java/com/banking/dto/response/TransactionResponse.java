package com.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String referenceId;
    private String entryType; // DEBIT or CREDIT
    private BigDecimal amount;
    private BigDecimal resultingBalance;
    private String currency;
    private String transferType; // TRANSFER, DEPOSIT, WITHDRAWAL
    private String status;
    private String description;
    private String counterpartyAccountNumber;
    private String counterpartyMaskedAccountNumber;
    private String counterpartyName;
    private LocalDateTime timestamp;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String referenceId, String entryType, BigDecimal amount,
                               BigDecimal resultingBalance, String currency, String transferType,
                               String status, String description, String counterpartyAccountNumber,
                               String counterpartyMaskedAccountNumber, String counterpartyName,
                               LocalDateTime timestamp) {
        this.id = id;
        this.referenceId = referenceId;
        this.entryType = entryType;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.currency = currency;
        this.transferType = transferType;
        this.status = status;
        this.description = description;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.counterpartyMaskedAccountNumber = counterpartyMaskedAccountNumber;
        this.counterpartyName = counterpartyName;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(BigDecimal resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public void setCounterpartyAccountNumber(String counterpartyAccountNumber) {
        this.counterpartyAccountNumber = counterpartyAccountNumber;
    }

    public String getCounterpartyMaskedAccountNumber() {
        return counterpartyMaskedAccountNumber;
    }

    public void setCounterpartyMaskedAccountNumber(String counterpartyMaskedAccountNumber) {
        this.counterpartyMaskedAccountNumber = counterpartyMaskedAccountNumber;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
