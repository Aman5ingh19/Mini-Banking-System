package com.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferReceiptResponse {

    private String referenceId;
    private String sourceAccountNumber;
    private String sourceMaskedAccountNumber;
    private String destinationAccountNumber;
    private String destinationMaskedAccountNumber;
    private String senderName;
    private String recipientName;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String status;
    private String description;
    private BigDecimal resultingBalance;
    private LocalDateTime timestamp;

    public TransferReceiptResponse() {
    }

    public TransferReceiptResponse(String referenceId, String sourceAccountNumber, String sourceMaskedAccountNumber,
                                   String destinationAccountNumber, String destinationMaskedAccountNumber,
                                   String senderName, String recipientName, BigDecimal amount,
                                   String currency, String type, String status, String description,
                                   BigDecimal resultingBalance, LocalDateTime timestamp) {
        this.referenceId = referenceId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.sourceMaskedAccountNumber = sourceMaskedAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.destinationMaskedAccountNumber = destinationMaskedAccountNumber;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.description = description;
        this.resultingBalance = resultingBalance;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getSourceMaskedAccountNumber() {
        return sourceMaskedAccountNumber;
    }

    public void setSourceMaskedAccountNumber(String sourceMaskedAccountNumber) {
        this.sourceMaskedAccountNumber = sourceMaskedAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public String getDestinationMaskedAccountNumber() {
        return destinationMaskedAccountNumber;
    }

    public void setDestinationMaskedAccountNumber(String destinationMaskedAccountNumber) {
        this.destinationMaskedAccountNumber = destinationMaskedAccountNumber;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(BigDecimal resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
