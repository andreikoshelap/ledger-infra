package com.gattopiccolo.ledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("demo")
public class NoOpExternalLoggingClient implements ExternalLoggingClient {
    private static final Logger log = LoggerFactory.getLogger(NoOpExternalLoggingClient.class);

    @Override
    public void logBeforeDebit(Long accountId, BigDecimal amount) {
        log.info("[demo] external pre-debit log skipped: account={} amount={}", accountId, amount);
    }
}