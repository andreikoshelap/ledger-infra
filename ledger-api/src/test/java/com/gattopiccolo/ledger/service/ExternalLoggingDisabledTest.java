package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "ledger.external-logging.enabled=false")
class ExternalLoggingDisabledTest {

    @Autowired
    private AccountService service;

    @Test
    void debitProceedsWhenExternalLoggingIsDisabled() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        service.debit(id, new BigDecimal("40.00"));

        assertEquals("60.00", service.getBalance(id).balance());
    }
}
