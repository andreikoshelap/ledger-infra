package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.service.AccountService;
import com.gattopiccolo.ledger.service.ExternalLoggingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end through the engine, correlation layer and database (without HTTP).
 * Each test uses its own actor id so the shared Spring context stays isolated.
 */
@SpringBootTest
class DetectionPipelineTest {

    @Autowired
    private DetectionEngine engine;

    @Autowired
    private FindingService findings;

    @Autowired
    private AccountService accounts;

    // Never touch the real external endpoint.
    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    private SecurityEvent foreignRead(String actor, String ip, Long accountId, Long owner, Instant at) {
        return new SecurityEvent(at, actor, ip, "GET", "/api/accounts/" + accountId + "/balance",
                200, accountId, owner, EventType.FOREIGN_ACCOUNT_ACCESS);
    }

    private List<Finding> findingsOf(String actor) {
        return findings.list(null, 0).stream().filter(f -> actor.equals(f.getActor())).toList();
    }

    @Test
    void repeatedForeignAccessFoldsIntoOneFindingWithAnOccurrenceCount() {
        Long victim = accounts.openAccount(9001L, CurrencyCode.EUR);
        Instant t0 = Instant.now();

        for (int i = 0; i < 5; i++) {
            engine.onEvent(foreignRead("9002", "198.51.100.7", victim, 9001L, t0.plusSeconds(i)));
        }

        List<Finding> mine = findingsOf("9002").stream()
                .filter(f -> "LEDGER-001".equals(f.getRuleId())).toList();
        assertEquals(1, mine.size());
        assertEquals(5, mine.get(0).getOccurrenceCount());
        assertEquals(Severity.HIGH, mine.get(0).getSeverity());
        assertEquals(FindingStatus.OPEN, mine.get(0).getStatus());
    }

    @Test
    void knownMaliciousSourceScoresHigherThanAnUnknownOne() {
        Long victim = accounts.openAccount(9003L, CurrencyCode.EUR);
        Instant now = Instant.now();

        engine.onEvent(foreignRead("9004", "198.51.100.8", victim, 9003L, now));
        engine.onEvent(foreignRead("9005", "203.0.113.66", victim, 9003L, now));

        int unknown = findingsOf("9004").get(0).getRiskScore();
        Finding bad = findingsOf("9005").get(0);
        assertTrue(bad.getRiskScore() > unknown);
        assertTrue(bad.getIntel().startsWith("MALICIOUS"));
    }

    @Test
    void resolvedFindingIsNotReusedForNewDetections() {
        Long victim = accounts.openAccount(9006L, CurrencyCode.EUR);
        Instant now = Instant.now();

        engine.onEvent(foreignRead("9007", "198.51.100.9", victim, 9006L, now));
        Finding first = findingsOf("9007").get(0);
        findings.updateStatus(first.getId(), FindingStatus.RESOLVED);

        engine.onEvent(foreignRead("9007", "198.51.100.9", victim, 9006L, now.plusSeconds(5)));

        assertEquals(2, findingsOf("9007").stream()
                .filter(f -> "LEDGER-001".equals(f.getRuleId())).count());
    }

    @Test
    void summaryReportsNoiseReduction() {
        Long victim = accounts.openAccount(9008L, CurrencyCode.EUR);
        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            engine.onEvent(foreignRead("9009", "198.51.100.10", victim, 9008L, now.plusSeconds(i)));
        }

        FindingService.Summary summary = findings.summary(engine.window().total());

        assertTrue(summary.rawEvents() >= 20);
        assertTrue(summary.suppressedDuplicates() >= 19);
        assertTrue(summary.noiseReductionPct() > 0.0);
    }
}
