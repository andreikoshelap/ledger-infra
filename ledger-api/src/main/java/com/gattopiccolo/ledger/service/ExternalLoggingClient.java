package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.exception.ExternalLoggingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Simulates the mandatory "call an external system before debiting" requirement
 * by hitting a configurable web endpoint (e.g. https://httpstat.us/200).
 *
 * Important: this is invoked from the service layer BEFORE the database
 * transaction opens, so a slow or failing external system never blocks while a
 * pessimistic row lock is held. A non-2xx response or timeout aborts the debit.
 */
@Component
public class ExternalLoggingClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalLoggingClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String path;

    public ExternalLoggingClient(RestClient externalLoggingRestClient,
                                 @Value("${ledger.external-logging.enabled:false}") boolean enabled,
                                 @Value("${ledger.external-logging.path:/200}") String path) {
        this.restClient = externalLoggingRestClient;
        this.enabled = enabled;
        this.path = path;
    }

    public void logBeforeDebit(Long accountId, BigDecimal amount) {
        if (!enabled) {
            log.debug("External logging disabled; skipping debit intent log for account={}, amount={}",
                    accountId, amount);
            return;
        }
        try {
            restClient.get()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("External logging acknowledged debit intent: account={}, amount={}", accountId, amount);
        } catch (RestClientException e) {
            throw new ExternalLoggingException(
                    "External logging call failed before debit on account " + accountId, e);
        }
    }
}
