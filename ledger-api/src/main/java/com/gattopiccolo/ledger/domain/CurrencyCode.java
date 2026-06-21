package com.gattopiccolo.ledger.domain;

import com.gattopiccolo.ledger.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Supported currencies. Each carries:
 *  - fractionDigits: the number of minor units (VND has none, the rest have two).
 *  - eurPerUnit:     fixed reference rate (1 unit of this currency expressed in EUR),
 *                    used by {@link com.gattopiccolo.ledger.money.ExchangeRateProvider}.
 */
public enum CurrencyCode {

    EUR(2, new BigDecimal("1.0")),
    USD(2, new BigDecimal("0.92")),
    SEK(2, new BigDecimal("0.087")),
    GBP(2, new BigDecimal("1.17")),
    VND(0, new BigDecimal("0.000037"));

    private final int fractionDigits;
    private final BigDecimal eurPerUnit;

    CurrencyCode(int fractionDigits, BigDecimal eurPerUnit) {
        this.fractionDigits = fractionDigits;
        this.eurPerUnit = eurPerUnit;
    }

    public int getFractionDigits() {
        return fractionDigits;
    }

    public BigDecimal eurPerUnit() {
        return eurPerUnit;
    }

    /**
     * Validates that {@code amount} has no more precision than this currency allows
     * (e.g. VND must be a whole number) and returns it normalised to the exact scale.
     * Rejects rather than silently rounds user-supplied amounts.
     */
    public BigDecimal requireValidScale(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        int effectiveScale = Math.max(amount.stripTrailingZeros().scale(), 0);
        if (effectiveScale > fractionDigits) {
            throw new InvalidAmountException(
                    "Currency " + name() + " allows at most " + fractionDigits + " decimal place(s)");
        }
        return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
    }

    public BigDecimal round(BigDecimal amount) {
        return amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    }

    public int displayScale() {
        return this == VND ? 0 : 2;
    }
}
