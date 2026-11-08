package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;

import java.time.Duration;
import java.util.Optional;

/**
 * Many 4xx responses for one actor in a short time: fuzzing, scanning or a
 * broken client. Deliberately low severity; it is context, not an alarm.
 */
public final class ClientErrorProbingRule implements DetectionRule {

    public static final String ID = "LEDGER-004";

    private final Duration windowSize;
    private final int threshold;

    public ClientErrorProbingRule(Duration windowSize, int threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent latest, EventWindow window) {
        if (!latest.isClientError()) {
            return Optional.empty();
        }
        long count = window.byActorSince(latest.actor(), latest.at().minus(windowSize)).stream()
                .filter(SecurityEvent::isClientError)
                .count();
        if (count < threshold) {
            return Optional.empty();
        }
        Severity severity = count >= threshold * 2L ? Severity.MEDIUM : Severity.LOW;
        String evidence = String.format("%d client errors (4xx) for user %s within %ds (threshold %d)",
                count, latest.actor(), windowSize.toSeconds(), threshold);
        return Optional.of(new Detection(
                ID,
                "Repeated client errors (possible probing)",
                severity,
                "A06:2025 Insecure Design (no rate limiting)",
                "Reconnaissance (TA0043)",
                "T1595 Active Scanning",
                latest.actor(),
                latest.sourceIp(),
                latest.accountId(),
                evidence,
                latest.at()));
    }
}
