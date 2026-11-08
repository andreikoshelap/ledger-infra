package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.repository.AccountRepository;
import com.gattopiccolo.ledger.threat.enrich.IpReputation;
import com.gattopiccolo.ledger.threat.enrich.ThreatIntelProvider;
import com.gattopiccolo.ledger.threat.rule.Detection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Correlation layer: enriches each detection with threat intel and asset
 * context, scores it, and either opens a new finding or folds it into an
 * existing active one. The ratio of raw events to findings is the prototype's
 * answer to alert fatigue.
 */
@Service
public class FindingService {

    private static final List<FindingStatus> ACTIVE = List.of(FindingStatus.OPEN, FindingStatus.ACKNOWLEDGED);

    private final FindingRepository findings;
    private final AccountRepository accounts;
    private final ThreatIntelProvider intel;
    private final SecurityProperties props;

    private final AtomicLong detections = new AtomicLong();
    private final AtomicLong suppressed = new AtomicLong();

    public FindingService(FindingRepository findings,
                          AccountRepository accounts,
                          ThreatIntelProvider intel,
                          SecurityProperties props) {
        this.findings = findings;
        this.accounts = accounts;
        this.intel = intel;
        this.props = props;
    }

    @Transactional
    public Finding raise(Detection d) {
        detections.incrementAndGet();

        IpReputation reputation = intel.lookup(d.sourceIp());
        double assetMultiplier = d.affectedAccountId() == null
                ? 1.0
                : accounts.findById(d.affectedAccountId())
                        .map(a -> RiskScorer.assetMultiplier(a.getBalance()))
                        .orElse(1.0);
        int score = RiskScorer.score(d.severity(), assetMultiplier, reputation);

        Instant cutoff = d.at().minus(props.suppression());
        Optional<Finding> existing = findings
                .findFirstByFingerprintAndStatusInAndLastSeenAfterOrderByLastSeenDesc(
                        d.fingerprint(), ACTIVE, cutoff);
        if (existing.isPresent()) {
            suppressed.incrementAndGet();
            Finding finding = existing.get();
            finding.recordOccurrence(d.at(), d.severity(), score, d.evidence());
            return finding; // managed entity: flushed on commit
        }
        return findings.save(Finding.from(d, score, reputation.summary()));
    }

    @Transactional(readOnly = true)
    public List<Finding> list(FindingStatus status, int minScore) {
        List<Finding> all = status == null
                ? findings.findAllByOrderByRiskScoreDescLastSeenDesc()
                : findings.findByStatusOrderByRiskScoreDescLastSeenDesc(status);
        return all.stream().filter(f -> f.getRiskScore() >= minScore).toList();
    }

    @Transactional(readOnly = true)
    public Finding get(Long id) {
        return findings.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Finding updateStatus(Long id, FindingStatus status) {
        Finding finding = findings.findById(id).orElseThrow(() -> notFound(id));
        finding.setStatus(status);
        return finding;
    }

    @Transactional(readOnly = true)
    public Summary summary(long rawEvents) {
        long detected = detections.get();
        long folded = suppressed.get();
        long created = detected - folded;
        double reduction = rawEvents == 0 ? 0.0 : 100.0 * (1.0 - (double) created / rawEvents);
        return new Summary(rawEvents, detected, folded, created,
                findings.countByStatusIn(ACTIVE), Math.round(reduction * 10) / 10.0);
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding " + id + " not found");
    }

    /**
     * Noise metrics since application start.
     *
     * @param rawEvents          events seen by the sensor
     * @param detections         rule hits (a single event can hit several rules)
     * @param suppressedDuplicates detections folded into an existing finding
     * @param findingsCreated    new findings opened
     * @param activeFindings     findings currently open or acknowledged
     * @param noiseReductionPct  100 x (1 - findingsCreated / rawEvents)
     */
    public record Summary(long rawEvents,
                          long detections,
                          long suppressedDuplicates,
                          long findingsCreated,
                          long activeFindings,
                          double noiseReductionPct) {
    }
}
