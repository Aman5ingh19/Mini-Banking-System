package com.banking.exception;

public class UserAlreadyExistsException extends BankingException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
