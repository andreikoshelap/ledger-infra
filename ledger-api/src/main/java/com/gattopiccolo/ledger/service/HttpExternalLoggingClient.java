package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.exception.ExternalLoggingException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@Profile("!demo")
public class HttpExternalLoggingClient implements ExternalLoggingClient {

    private final RestClient http;

    public HttpExternalLoggingClient() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(1500));   // fast fail
        f.setReadTimeout(Duration.ofMillis(1500));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    @Override
    public void logBeforeDebit(Long accountId, BigDecimal amount) {
        try {
            http.get().uri("https://httpstat.us/200").retrieve().toBodilessEntity();
        } catch (Exception ex) {                         // connect timeout, 5xx
            throw new ExternalLoggingException("External logging call failed before " +
                    "debit on account" +accountId, ex);
        }
    }
}