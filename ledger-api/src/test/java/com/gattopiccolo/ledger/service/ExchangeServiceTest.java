package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.domain.TransactionType;
import com.gattopiccolo.ledger.exception.AccountNotFoundException;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import com.gattopiccolo.ledger.exception.InvalidAmountException;
import com.gattopiccolo.ledger.service.view.ExchangeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ExchangeServiceTest {

    @Autowired
    private AccountService service;

    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void exchangeDebitsSourceAndCreditsConvertedTarget() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);
        Long usd = service.openAccount(1L, CurrencyCode.USD);
        service.deposit(eur, new BigDecimal("100.00"));

        ExchangeResult result = service.exchange(1L, eur, usd, new BigDecimal("50.00"));

        assertEquals("50.00", service.getBalance(eur).balance());
        // 50 EUR -> 50 / 0.92 = 54.3478... -> 54.35 USD
        assertEquals("54.35", service.getBalance(usd).balance());
        assertEquals(TransactionType.EXCHANGE_OUT, result.out().type());
        assertEquals(TransactionType.EXCHANGE_IN, result.in().type());
        assertEquals("-50.00", result.out().amount());
        assertEquals("54.35", result.in().amount());
        verify(externalLoggingClient).logBeforeDebit(eur, new BigDecimal("50.00"));
    }

    @Test
    void exchangeWithInsufficientFundsThrowsAndRollsBack() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);
        Long usd = service.openAccount(1L, CurrencyCode.USD);
        service.deposit(eur, new BigDecimal("10.00"));

        assertThrows(InsufficientFundsException.class,
                () -> service.exchange(1L, eur, usd, new BigDecimal("50.00")));

        assertEquals("10.00", service.getBalance(eur).balance());
        assertEquals("0.00", service.getBalance(usd).balance());
    }

    @Test
    void exchangeRejectsSameSourceAndTargetAccount() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);

        assertThrows(InvalidAmountException.class,
                () -> service.exchange(1L, eur, eur, new BigDecimal("10.00")));
    }

    @Test
    void exchangeRejectsNonPositiveAmount() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);
        Long usd = service.openAccount(1L, CurrencyCode.USD);

        assertThrows(InvalidAmountException.class, () -> service.exchange(1L, eur, usd, BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class,
                () -> service.exchange(1L, eur, usd, new BigDecimal("-1.00")));
    }

    @Test
    void exchangeFailsWhenSourceAccountDoesNotExist() {
        Long usd = service.openAccount(1L, CurrencyCode.USD);

        assertThrows(AccountNotFoundException.class,
                () -> service.exchange(1L, 999_999L, usd, new BigDecimal("10.00")));
    }

    @Test
    void exchangeFailsWhenAccountBelongsToAnotherUser() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);
        Long usd = service.openAccount(2L, CurrencyCode.USD);
        service.deposit(eur, new BigDecimal("10.00"));

        assertThrows(AccountNotFoundException.class,
                () -> service.exchange(1L, eur, usd, new BigDecimal("5.00")));
    }
}
