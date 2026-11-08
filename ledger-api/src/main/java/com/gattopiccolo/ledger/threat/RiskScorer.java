package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.threat.enrich.IpReputation;

import java.math.BigDecimal;

/**
 * Turns a detection plus its context into a 0..100 risk score.
 *
 * <p>This is the prototype's "findings prioritisation with context":
 * {@code score = baseScore(severity) x assetMultiplier x intelMultiplier},
 * capped at 100. A teaching heuristic, not a standard formula.
 */
public final class RiskScorer {

    private static final BigDecimal HIGH_VALUE = new BigDecimal("10000");
    private static final BigDecimal MEDIUM_VALUE = new BigDecimal("1000");

    private RiskScorer() {
    }

    public static int score(Severity severity, double assetMultiplier, IpReputation intel) {
        double value = severity.baseScore() * assetMultiplier;
        if (intel != null && intel.verdict() == IpReputation.Verdict.MALICIOUS) {
            value *= 1.5;
        }
        return (int) Math.min(100, Math.round(value));
    }

    /** Larger balances make the affected account a more valuable target. */
    public static double assetMultiplier(BigDecimal balance) {
        if (balance == null) {
            return 1.0;
        }
        if (balance.compareTo(HIGH_VALUE) >= 0) {
            return 1.5;
        }
        if (balance.compareTo(MEDIUM_VALUE) >= 0) {
            return 1.2;
        }
        return 1.0;
    }
}
