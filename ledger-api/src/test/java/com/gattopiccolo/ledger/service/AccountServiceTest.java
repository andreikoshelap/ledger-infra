package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import com.gattopiccolo.ledger.service.view.TransactionPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService service;

    // No-op so these tests never touch the real external endpoint.
    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void depositIncreasesBalance() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), service.getBalance(id).balance());
    }

    @Test
    void debitReducesBalance() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));
        service.debit(id, new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), service.getBalance(id).balance());
    }

    @Test
    void debitWithInsufficientFundsThrowsAndLeavesBalanceUnchanged() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("20.00"));
        assertThrows(InsufficientFundsException.class, () -> service.debit(id, new BigDecimal("50.00")));
        assertEquals(new BigDecimal("20.00"), service.getBalance(id).balance());
    }

    @Test
    void historyIsNewestFirstAndPagesViaCursor() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        for (int i = 1; i <= 5; i++) {
            service.deposit(id, new BigDecimal(i + ".00"));
        }

        TransactionPage page1 = service.history(id, null, 2);
        assertEquals(2, page1.items().size());
        assertNotNull(page1.nextCursor());
        assertTrue(page1.items().get(0).id() > page1.items().get(1).id());

        TransactionPage page2 = service.history(id, page1.nextCursor(), 2);
        assertEquals(2, page2.items().size());
        assertTrue(page2.items().get(0).id() < page1.items().get(1).id());
    }
}
