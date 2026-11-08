package com.gattopiccolo.ledger.threat;

/**
 * Severity of a detection. The base score is the starting point of the risk
 * score computed by {@link RiskScorer}; context (asset value, threat intel)
 * then raises it.
 */
public enum Severity {
    LOW(10),
    MEDIUM(30),
    HIGH(60),
    CRITICAL(90);

    private final int baseScore;

    Severity(int baseScore) {
        this.baseScore = baseScore;
    }

    public int baseScore() {
        return baseScore;
    }
}
