package com.gattopiccolo.ledger.threat;

/** Classification assigned by the sensor ({@link SecurityEventInterceptor}). */
public enum EventType {
    /** Ordinary request, kept only as investigation context. */
    REQUEST,
    /** The caller touched an account that belongs to a different user. */
    FOREIGN_ACCOUNT_ACCESS,
    /** A debit call on the caller's own account. */
    DEBIT,
    /** Any other 4xx response. */
    CLIENT_ERROR
}
