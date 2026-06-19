package com.gattopiccolo.ledger.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId) {
        super("Insufficient funds on account: " + accountId);
    }
}
