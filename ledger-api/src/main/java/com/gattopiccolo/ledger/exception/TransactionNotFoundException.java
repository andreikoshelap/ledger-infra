package com.gattopiccolo.ledger.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long entryId) {
        super("Transaction not found: " + entryId);
    }
}
