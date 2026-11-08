package com.gattopiccolo.ledger.threat.web;

import com.gattopiccolo.ledger.threat.DetectionEngine;
import com.gattopiccolo.ledger.threat.Finding;
import com.gattopiccolo.ledger.threat.FindingService;
import com.gattopiccolo.ledger.threat.FindingStatus;
import com.gattopiccolo.ledger.threat.SecurityEvent;
import com.gattopiccolo.ledger.threat.Severity;
import com.gattopiccolo.ledger.threat.vuln.PrioritizedVulnerability;
import com.gattopiccolo.ledger.threat.vuln.SampleVulnerabilityCatalog;
import com.gattopiccolo.ledger.threat.vuln.VulnerabilityPrioritizer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Read-mostly API behind the Security page. Effective paths are prefixed with
 * {@code /api} by {@link com.gattopiccolo.ledger.config.WebMvcConfig}.
 *
 * <p>Like the rest of this prototype it has no real authentication; do not
 * expose it outside a trusted network.
 */
@RestController
@RequestMapping("/security")
public class ThreatController {

    private static final int MAX_EVENTS = 200;

    private final FindingService findings;
    private final DetectionEngine engine;
    private final VulnerabilityPrioritizer prioritizer;

    public ThreatController(FindingService findings,
                            DetectionEngine engine,
                            VulnerabilityPrioritizer prioritizer) {
        this.findings = findings;
        this.engine = engine;
        this.prioritizer = prioritizer;
    }

    @GetMapping("/findings")
    public List<FindingView> listFindings(@RequestParam(required = false) FindingStatus status,
                                          @RequestParam(defaultValue = "0") int minScore) {
        return findings.list(status, minScore).stream().map(FindingView::of).toList();
    }

    @GetMapping("/findings/{id}")
    public FindingView getFinding(@PathVariable Long id) {
        return FindingView.of(findings.get(id));
    }

    @PostMapping("/findings/{id}/status")
    public FindingView updateStatus(@PathVariable Long id, @RequestBody StatusRequest body) {
        return FindingView.of(findings.updateStatus(id, body.status()));
    }

    @GetMapping("/events")
    public List<SecurityEvent> recentEvents(@RequestParam(defaultValue = "50") int limit,
                                            @RequestParam(required = false) String actor) {
        int bounded = Math.min(Math.max(limit, 1), MAX_EVENTS);
        return engine.window().recent(bounded, actor);
    }

    @GetMapping("/summary")
    public FindingService.Summary summary() {
        return findings.summary(engine.window().total());
    }

    @GetMapping("/vulnerabilities")
    public List<PrioritizedVulnerability> vulnerabilities() {
        return prioritizer.rank(SampleVulnerabilityCatalog.findings());
    }

    public record StatusRequest(FindingStatus status) {
    }

    public record FindingView(Long id,
                              String ruleId,
                              String title,
                              Severity severity,
                              int riskScore,
                              FindingStatus status,
                              String owaspCategory,
                              String attackTactic,
                              String attackTechnique,
                              String actor,
                              String sourceIp,
                              Long affectedAccountId,
                              int occurrenceCount,
                              Instant firstSeen,
                              Instant lastSeen,
                              String evidence,
                              String intel) {

        static FindingView of(Finding f) {
            return new FindingView(f.getId(), f.getRuleId(), f.getTitle(), f.getSeverity(),
                    f.getRiskScore(), f.getStatus(), f.getOwaspCategory(), f.getAttackTactic(),
                    f.getAttackTechnique(), f.getActor(), f.getSourceIp(), f.getAffectedAccountId(),
                    f.getOccurrenceCount(), f.getFirstSeen(), f.getLastSeen(), f.getEvidence(), f.getIntel());
        }
    }
}
