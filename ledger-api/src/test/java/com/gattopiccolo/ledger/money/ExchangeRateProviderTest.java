package com.gattopiccolo.ledger.money;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeRateProviderTest {

    private final ExchangeRateProvider rates = new ExchangeRateProvider();

    @Test
    void convertsEurToUsdWithBankersRounding() {
        // 100 EUR -> 100 / 0.92 = 108.6956... -> 108.70 USD
        BigDecimal result = rates.convert(new BigDecimal("100.00"), CurrencyCode.EUR, CurrencyCode.USD);
        assertEquals(new BigDecimal("108.70"), result);
    }

    @Test
    void convertsToVndAtZeroScale() {
        BigDecimal result = rates.convert(new BigDecimal("1.00"), CurrencyCode.EUR, CurrencyCode.VND);
        assertEquals(0, result.scale());
    }

    @Test
    void sameCurrencyReturnsNormalisedAmount() {
        assertEquals(new BigDecimal("25.00"),
                rates.convert(new BigDecimal("25"), CurrencyCode.EUR, CurrencyCode.EUR));
    }
}
