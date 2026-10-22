package com.banking.exception;

public class InvalidTransferException extends BankingException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
