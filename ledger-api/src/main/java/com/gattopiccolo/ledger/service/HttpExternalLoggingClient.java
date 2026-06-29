package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.exception.ExternalLoggingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@Profile("!demo & (prod | postman)")
public class HttpExternalLoggingClient implements ExternalLoggingClient {

    private final RestClient http;
    private final String path;

    public HttpExternalLoggingClient(RestClient externalLoggingRestClient,
                                     @Value("${ledger.external-logging.path}") String path) {
        this.http = externalLoggingRestClient;
        this.path = path;
    }

    @Override
    public void logBeforeDebit(Long accountId, BigDecimal amount) {
        try {
            http.get().uri(path).retrieve().toBodilessEntity();
        } catch (Exception ex) {                         // connect timeout, 5xx
            throw new ExternalLoggingException("External logging call failed before " +
                    "debit on account " +accountId, ex);
        }
    }
}
