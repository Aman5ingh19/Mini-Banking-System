package com.banking.exception;

public class DuplicateIdempotencyKeyException extends BankingException {
    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}
