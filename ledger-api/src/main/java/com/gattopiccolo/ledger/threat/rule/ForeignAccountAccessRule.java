package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventType;
import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;

import java.util.Optional;

/**
 * Broken object level authorization (BOLA / IDOR): a caller addressed an
 * account that belongs to somebody else.
 *
 * <p>Severity follows impact: a successful state-changing call is critical, a
 * successful read exposes data, anything else is an attempt.
 */
public final class ForeignAccountAccessRule implements DetectionRule {

    public static final String ID = "LEDGER-001";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Detection> evaluate(SecurityEvent latest, EventWindow window) {
        if (latest.type() != EventType.FOREIGN_ACCOUNT_ACCESS) {
            return Optional.empty();
        }
        Severity severity;
        if (!latest.isSuccess()) {
            severity = Severity.MEDIUM;
        } else if (latest.isMutation()) {
            severity = Severity.CRITICAL;
        } else {
            severity = Severity.HIGH;
        }
        String evidence = String.format("user %s %s %s on account %s owned by user %s -> HTTP %d",
                latest.actor(), latest.method(), latest.path(),
                latest.accountId(), latest.ownerUserId(), latest.status());
        return Optional.of(new Detection(
                ID,
                "Access to another user's account (BOLA)",
                severity,
                "A01:2025 Broken Access Control",
                "Initial Access (TA0001)",
                "T1190 Exploit Public-Facing Application",
                latest.actor(),
                latest.sourceIp(),
                latest.accountId(),
                evidence,
                latest.at()));
    }
}
