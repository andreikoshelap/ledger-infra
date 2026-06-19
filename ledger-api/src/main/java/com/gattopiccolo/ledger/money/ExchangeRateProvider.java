package com.gattopiccolo.ledger.money;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fixed-rate currency conversion. Every currency declares a reference rate
 * against EUR, so any pair is converted via EUR as the pivot:
 *   amountInEur = amount * eurPer(from)
 *   result = amountInEur / eurPer(to)
 *   * The intermediate division keeps extra precision; the final value is rounded
 * to the target currency's scale with banker's rounding (HALF_EVEN).
 */
@Component
public class ExchangeRateProvider {

    private static final int INTERMEDIATE_SCALE = 12;

    public BigDecimal convert(BigDecimal amount, CurrencyCode from, CurrencyCode to) {
        BigDecimal normalised = from.requireValidScale(amount);
        if (from == to) {
            return normalised;
        }
        BigDecimal amountInEur = normalised.multiply(from.eurPerUnit());
        BigDecimal converted = amountInEur.divide(to.eurPerUnit(), INTERMEDIATE_SCALE, RoundingMode.HALF_EVEN);
        return converted.setScale(to.getFractionDigits(), RoundingMode.HALF_EVEN);
    }

    /** Exposed for inspection/UI: how many units of {@code to} one unit of {@code from} buys. */
    public BigDecimal rate(CurrencyCode from, CurrencyCode to) {
        return from.eurPerUnit().divide(to.eurPerUnit(), INTERMEDIATE_SCALE, RoundingMode.HALF_EVEN);
    }
}
