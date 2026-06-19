package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.domain.TransactionType;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import com.gattopiccolo.ledger.service.view.ExchangeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        ExchangeResult result = service.exchange(eur, usd, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), service.getBalance(eur).balance());
        // 50 EUR -> 50 / 0.92 = 54.3478... -> 54.35 USD
        assertEquals(new BigDecimal("54.35"), service.getBalance(usd).balance());
        assertEquals(TransactionType.EXCHANGE_OUT, result.out().type());
        assertEquals(TransactionType.EXCHANGE_IN, result.in().type());
    }

    @Test
    void exchangeWithInsufficientFundsThrowsAndRollsBack() {
        Long eur = service.openAccount(1L, CurrencyCode.EUR);
        Long usd = service.openAccount(1L, CurrencyCode.USD);
        service.deposit(eur, new BigDecimal("10.00"));

        assertThrows(InsufficientFundsException.class,
                () -> service.exchange(eur, usd, new BigDecimal("50.00")));

        assertEquals(new BigDecimal("10.00"), service.getBalance(eur).balance());
        assertEquals(new BigDecimal("0.00"), service.getBalance(usd).balance());
    }
}
