package com.gattopiccolo.ledger.domain;

import com.gattopiccolo.ledger.exception.InvalidAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyCodeTest {

    @Test
    void eurNormalisesToTwoDecimals() {
        assertEquals(new BigDecimal("10.50"), CurrencyCode.EUR.requireValidScale(new BigDecimal("10.5")));
    }

    @Test
    void eurRejectsThreeDecimals() {
        assertThrows(InvalidAmountException.class,
                () -> CurrencyCode.EUR.requireValidScale(new BigDecimal("10.501")));
    }

    @Test
    void vndRejectsAnyFraction() {
        assertThrows(InvalidAmountException.class,
                () -> CurrencyCode.VND.requireValidScale(new BigDecimal("10.5")));
    }

    @Test
    void vndAcceptsWholeNumberAtZeroScale() {
        BigDecimal normalised = CurrencyCode.VND.requireValidScale(new BigDecimal("10"));
        assertEquals(0, normalised.scale());
        assertEquals(new BigDecimal("10"), normalised);
    }
}
