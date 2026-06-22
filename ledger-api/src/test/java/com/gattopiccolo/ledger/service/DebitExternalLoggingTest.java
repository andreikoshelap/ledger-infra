package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.exception.ExternalLoggingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Uses a real ExternalLoggingClient pointed at WireMock to prove the external
 * call happens BEFORE the debit and gates it: a 500 from the external system
 * must abort the debit with the balance untouched.
 */
@SpringBootTest
class DebitExternalLoggingTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    static {
        // Started before the Spring context resolves the base-url property below.
        WIRE_MOCK.start();
    }

    @Autowired
    private AccountService service;

    @DynamicPropertySource
    static void externalLoggingProperties(DynamicPropertyRegistry registry) {
        registry.add("ledger.external-logging.enabled", () -> true);
        registry.add("ledger.external-logging.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
        registry.add("ledger.external-logging.path", () -> "/200");
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void resetStubs() {
        WIRE_MOCK.resetAll();
    }

    @Test
    void debitProceedsWhenExternalLoggingReturns2xx() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/200")).willReturn(aResponse().withStatus(200)));

        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));
        service.debit(id, new BigDecimal("40.00"));

        assertEquals("60.00", service.getBalance(id).balance());
        WIRE_MOCK.verify(getRequestedFor(urlEqualTo("/200")));
    }

    @Test
    void debitAbortsWhenExternalLoggingFails() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/200")).willReturn(aResponse().withStatus(500)));

        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        assertThrows(ExternalLoggingException.class, () -> service.debit(id, new BigDecimal("40.00")));
        assertEquals("100.00", service.getBalance(id).balance());
    }
}
