package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventType;
import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionRulesTest {

    private static final Instant T0 = Instant.parse("2026-11-08T10:00:00Z");
    private static final Duration FIVE_MIN = Duration.ofMinutes(5);

    private static SecurityEvent event(int secondsFromStart, String actor, String method, int status,
                                       Long accountId, Long owner, EventType type) {
        return new SecurityEvent(T0.plusSeconds(secondsFromStart), actor, "198.51.100.7", method,
                "/api/accounts/" + accountId, status, accountId, owner, type);
    }

    private static Optional<Detection> feed(DetectionRule rule, EventWindow window, SecurityEvent e) {
        window.add(e);
        return rule.evaluate(e, window);
    }

    // ----- LEDGER-001 foreign account access -----------------------------

    @Test
    void foreignReadThatSucceedsIsHigh() {
        var rule = new ForeignAccountAccessRule();
        var d = feed(rule, new EventWindow(100),
                event(0, "7", "GET", 200, 1L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));

        assertTrue(d.isPresent());
        assertEquals(Severity.HIGH, d.get().severity());
        assertEquals("A01:2025 Broken Access Control", d.get().owaspCategory());
        assertTrue(d.get().attackTechnique().startsWith("T1190"));
    }

    @Test
    void foreignMutationThatSucceedsIsCritical() {
        var rule = new ForeignAccountAccessRule();
        var d = feed(rule, new EventWindow(100),
                event(0, "7", "POST", 200, 1L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));

        assertEquals(Severity.CRITICAL, d.orElseThrow().severity());
    }

    @Test
    void foreignAccessThatWasRejectedIsMedium() {
        var rule = new ForeignAccountAccessRule();
        var d = feed(rule, new EventWindow(100),
                event(0, "7", "GET", 403, 1L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));

        assertEquals(Severity.MEDIUM, d.orElseThrow().severity());
    }

    @Test
    void ownAccountAccessIsIgnored() {
        var rule = new ForeignAccountAccessRule();
        var d = feed(rule, new EventWindow(100),
                event(0, "1", "GET", 200, 1L, 1L, EventType.REQUEST));

        assertFalse(d.isPresent());
    }

    @Test
    void fingerprintIsStablePerRuleActorAndIp() {
        var rule = new ForeignAccountAccessRule();
        var window = new EventWindow(100);
        var first = feed(rule, window, event(0, "7", "GET", 200, 1L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));
        var second = feed(rule, window, event(10, "7", "GET", 200, 2L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));

        assertEquals(first.orElseThrow().fingerprint(), second.orElseThrow().fingerprint());
    }

    // ----- LEDGER-002 enumeration ----------------------------------------

    @Test
    void enumerationFiresOnlyAtThreshold() {
        var rule = new AccountEnumerationRule(FIVE_MIN, 3);
        var window = new EventWindow(100);

        assertFalse(feed(rule, window, event(0, "7", "GET", 200, 1L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS)).isPresent());
        assertFalse(feed(rule, window, event(1, "7", "GET", 404, 2L, null, EventType.CLIENT_ERROR)).isPresent());
        var d = feed(rule, window, event(2, "7", "GET", 200, 3L, 1L, EventType.FOREIGN_ACCOUNT_ACCESS));

        assertTrue(d.isPresent());
        assertEquals(Severity.HIGH, d.get().severity());
        assertTrue(d.get().evidence().contains("3 distinct account ids"));
    }

    @Test
    void enumerationEscalatesToCriticalAtDoubleThreshold() {
        var rule = new AccountEnumerationRule(FIVE_MIN, 2);
        var window = new EventWindow(100);
        Optional<Detection> last = Optional.empty();
        for (long id = 1; id <= 4; id++) {
            last = feed(rule, window, event((int) id, "7", "GET", 404, id, null, EventType.CLIENT_ERROR));
        }
        assertEquals(Severity.CRITICAL, last.orElseThrow().severity());
    }

    @Test
    void enumerationIgnoresProbesOutsideTheWindow() {
        var rule = new AccountEnumerationRule(FIVE_MIN, 2);
        var window = new EventWindow(100);
        feed(rule, window, event(0, "7", "GET", 404, 1L, null, EventType.CLIENT_ERROR));
        var d = feed(rule, window, event(600, "7", "GET", 404, 2L, null, EventType.CLIENT_ERROR));

        assertFalse(d.isPresent());
    }

    @Test
    void enumerationIsPerActor() {
        var rule = new AccountEnumerationRule(FIVE_MIN, 2);
        var window = new EventWindow(100);
        feed(rule, window, event(0, "7", "GET", 404, 1L, null, EventType.CLIENT_ERROR));
        var d = feed(rule, window, event(1, "8", "GET", 404, 2L, null, EventType.CLIENT_ERROR));

        assertFalse(d.isPresent());
    }

    // ----- LEDGER-003 debit velocity -------------------------------------

    @Test
    void debitVelocityFiresAtThresholdAndEscalates() {
        var rule = new DebitVelocityRule(FIVE_MIN, 3);
        var window = new EventWindow(100);
        Optional<Detection> d = Optional.empty();
        for (int i = 0; i < 3; i++) {
            d = feed(rule, window, event(i, "5", "POST", 200, 9L, 5L, EventType.DEBIT));
        }
        assertEquals(Severity.MEDIUM, d.orElseThrow().severity());

        for (int i = 3; i < 6; i++) {
            d = feed(rule, window, event(i, "5", "POST", 200, 9L, 5L, EventType.DEBIT));
        }
        assertEquals(Severity.HIGH, d.orElseThrow().severity());
    }

    @Test
    void failedDebitsDoNotCountTowardsVelocity() {
        var rule = new DebitVelocityRule(FIVE_MIN, 2);
        var window = new EventWindow(100);
        feed(rule, window, event(0, "5", "POST", 422, 9L, 5L, EventType.DEBIT));
        var d = feed(rule, window, event(1, "5", "POST", 422, 9L, 5L, EventType.DEBIT));

        assertFalse(d.isPresent());
    }

    // ----- LEDGER-004 client error probing -------------------------------

    @Test
    void clientErrorsAreLowSeverityContext() {
        var rule = new ClientErrorProbingRule(FIVE_MIN, 3);
        var window = new EventWindow(100);
        Optional<Detection> d = Optional.empty();
        for (int i = 0; i < 3; i++) {
            d = feed(rule, window, event(i, "7", "GET", 422, 1L, 7L, EventType.CLIENT_ERROR));
        }
        assertEquals(Severity.LOW, d.orElseThrow().severity());
    }

    @Test
    void successfulRequestNeverTriggersClientErrorRule() {
        var rule = new ClientErrorProbingRule(FIVE_MIN, 1);
        var d = feed(rule, new EventWindow(100), event(0, "7", "GET", 200, 1L, 7L, EventType.REQUEST));

        assertFalse(d.isPresent());
    }
}
