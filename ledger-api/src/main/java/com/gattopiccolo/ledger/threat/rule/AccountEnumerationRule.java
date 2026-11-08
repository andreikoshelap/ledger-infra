package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventType;
import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Sequential account-id guessing: one actor probes many account ids that are
 * either foreign or non-existent within a short time.
 */
public final class AccountEnumerationRule implements DetectionRule {

    public static final String ID = "LEDGER-002";

    private final Duration windowSize;
    private final int threshold;

    public AccountEnumerationRule(Duration windowSize, int threshold) {
        this.windowSize = windowSize;
        this.threshold = threshold;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent latest, EventWindow window) {
        if (!isProbe(latest)) {
            return Optional.empty();
        }
        Instant from = latest.at().minus(windowSize);
        Set<Long> probed = window.byActorSince(latest.actor(), from).stream()
                .filter(AccountEnumerationRule::isProbe)
                .map(SecurityEvent::accountId)
                .collect(Collectors.toCollection(TreeSet::new));
        if (probed.size() < threshold) {
            return Optional.empty();
        }
        Severity severity = probed.size() >= threshold * 2 ? Severity.CRITICAL : Severity.HIGH;
        String evidence = String.format("user %s probed %d distinct account ids within %ds: %s",
                latest.actor(), probed.size(), windowSize.toSeconds(), probed);
        return Optional.of(new Detection(
                ID,
                "Account enumeration",
                severity,
                "A01:2025 Broken Access Control",
                "Discovery (TA0007)",
                "T1087 Account Discovery",
                latest.actor(),
                latest.sourceIp(),
                latest.accountId(),
                evidence,
                latest.at()));
    }

    private static boolean isProbe(SecurityEvent e) {
        return e.accountId() != null
                && (e.type() == EventType.FOREIGN_ACCOUNT_ACCESS || e.status() == 404);
    }
}
