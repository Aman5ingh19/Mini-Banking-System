package com.banking.dto.response;

public class RecipientLookupResponse {

    private String accountNumber;
    private String maskedAccountNumber;
    private String recipientName;
    private String status;

    public RecipientLookupResponse() {
    }

    public RecipientLookupResponse(String accountNumber, String maskedAccountNumber, String recipientName, String status) {
        this.accountNumber = accountNumber;
        this.maskedAccountNumber = maskedAccountNumber;
        this.recipientName = recipientName;
        this.status = status;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
