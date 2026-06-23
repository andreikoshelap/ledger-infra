package com.gattopiccolo.ledger.service;

import java.math.BigDecimal;

public interface ExternalLoggingClient {
    /** Pre-debit external log. Throws ExternalLoggingException if the call fails (fail-closed). */
    void logBeforeDebit(Long accountId, BigDecimal amount);
}
