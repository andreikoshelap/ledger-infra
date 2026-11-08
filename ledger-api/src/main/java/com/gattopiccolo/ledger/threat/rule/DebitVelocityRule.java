package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventType;
import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;

import java.time.Duration;
import java.util.Optional;

/**
 * Burst of successful debits from one actor: a typical shape of money drain
 * after account takeover, or of an automated abuse of a missing rate limit.
 */
public final class DebitVelocityRule implements DetectionRule {

    public static final String ID = "LEDGER-003";

    private final Duration windowSize;
    private final int threshold;

    public DebitVelocityRule(Duration windowSize, int threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent latest, EventWindow window) {
        if (!isSuccessfulDebit(latest)) {
            return Optional.empty();
        }
        long count = window.byActorSince(latest.actor(), latest.at().minus(windowSize)).stream()
                .filter(DebitVelocityRule::isSuccessfulDebit)
                .count();
        if (count < threshold) {
            return Optional.empty();
        }
        Severity severity = count >= threshold * 2L ? Severity.HIGH : Severity.MEDIUM;
        String evidence = String.format("%d successful debits by user %s within %ds (threshold %d)",
                count, latest.actor(), windowSize.toSeconds(), threshold);
        return Optional.of(new Detection(
                ID,
                "Unusual debit velocity",
                severity,
                "A06:2025 Insecure Design (no rate limiting)",
                "Impact (TA0040)",
                "T1657 Financial Theft",
                latest.actor(),
                latest.sourceIp(),
                latest.accountId(),
                evidence,
                latest.at()));
    }

    private static boolean isSuccessfulDebit(SecurityEvent e) {
        return e.type() == EventType.DEBIT && e.isSuccess();
    }
}
