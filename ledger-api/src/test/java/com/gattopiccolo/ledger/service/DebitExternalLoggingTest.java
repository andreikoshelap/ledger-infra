package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.exception.ExternalLoggingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Proves the external pre-debit hook gates the debit: if the hook fails, no DB
 * mutation is performed.
 */
@SpringBootTest
class DebitExternalLoggingTest {

    @Autowired
    private AccountService service;

    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void debitProceedsWhenExternalLoggingSucceeds() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        service.debit(id, new BigDecimal("40.00"));

        assertEquals("60.00", service.getBalance(id).balance());
        verify(externalLoggingClient).logBeforeDebit(id, new BigDecimal("40.00"));
    }

    @Test
    void debitAbortsWhenExternalLoggingFails() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));
        doThrow(new ExternalLoggingException("External logging failed", new RuntimeException("boom")))
                .when(externalLoggingClient).logBeforeDebit(id, new BigDecimal("40.00"));

        assertThrows(ExternalLoggingException.class, () -> service.debit(id, new BigDecimal("40.00")));
        assertEquals("100.00", service.getBalance(id).balance());
    }
}
