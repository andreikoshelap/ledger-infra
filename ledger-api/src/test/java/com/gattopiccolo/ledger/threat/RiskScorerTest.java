package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.threat.enrich.IpReputation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskScorerTest {

    private static final IpReputation UNKNOWN = IpReputation.unknown("198.51.100.7");
    private static final IpReputation MALICIOUS =
            new IpReputation("203.0.113.66", IpReputation.Verdict.MALICIOUS, "sample-feed");

    @Test
    void baseScoreIsUsedWhenThereIsNoContext() {
        assertEquals(60, RiskScorer.score(Severity.HIGH, 1.0, UNKNOWN));
    }

    @Test
    void maliciousSourceRaisesScoreByHalf() {
        assertEquals(90, RiskScorer.score(Severity.HIGH, 1.0, MALICIOUS));
    }

    @Test
    void scoreIsCappedAtOneHundred() {
        assertEquals(100, RiskScorer.score(Severity.CRITICAL, 1.5, MALICIOUS));
    }

    @Test
    void assetMultiplierGrowsWithBalance() {
        assertEquals(1.0, RiskScorer.assetMultiplier(new BigDecimal("999.99")));
        assertEquals(1.2, RiskScorer.assetMultiplier(new BigDecimal("1000")));
        assertEquals(1.5, RiskScorer.assetMultiplier(new BigDecimal("10000")));
        assertEquals(1.0, RiskScorer.assetMultiplier(null));
    }
}
