package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.threat.rule.Detection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A deduplicated, enriched, scored security finding: what an analyst actually
 * looks at. Many {@link Detection}s with the same fingerprint fold into one
 * finding whose {@code occurrenceCount} keeps growing.
 */
@Entity
@Table(name = "security_finding")
public class Finding {

    private static final int MAX_EVIDENCE = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fingerprint", nullable = false, length = 200)
    private String fingerprint;

    @Column(name = "rule_id", nullable = false, length = 32)
    private String ruleId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private FindingStatus status;

    @Column(name = "owasp_category", length = 120)
    private String owaspCategory;

    @Column(name = "attack_tactic", length = 60)
    private String attackTactic;

    @Column(name = "attack_technique", length = 120)
    private String attackTechnique;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "affected_account_id")
    private Long affectedAccountId;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "evidence", length = MAX_EVIDENCE)
    private String evidence;

    @Column(name = "intel", length = 255)
    private String intel;

    protected Finding() {
        // for JPA
    }

    public static Finding from(Detection d, int riskScore, String intel) {
        Finding f = new Finding();
        f.fingerprint = d.fingerprint();
        f.ruleId = d.ruleId();
        f.title = d.title();
        f.severity = d.severity();
        f.riskScore = riskScore;
        f.status = FindingStatus.OPEN;
        f.owaspCategory = d.owaspCategory();
        f.attackTactic = d.attackTactic();
        f.attackTechnique = d.attackTechnique();
        f.actor = d.actor();
        f.sourceIp = d.sourceIp();
        f.affectedAccountId = d.affectedAccountId();
        f.occurrenceCount = 1;
        f.firstSeen = d.at();
        f.lastSeen = d.at();
        f.evidence = truncate(d.evidence());
        f.intel = intel;
        return f;
    }

    /** Folds a repeated detection into this finding; severity and score only ever go up. */
    public void recordOccurrence(Instant at, Severity newSeverity, int newScore, String newEvidence) {
        occurrenceCount++;
        if (at.isAfter(lastSeen)) {
            lastSeen = at;
        }
        if (newSeverity.compareTo(severity) > 0) {
            severity = newSeverity;
        }
        riskScore = Math.max(riskScore, newScore);
        evidence = truncate(newEvidence);
    }

    public void setStatus(FindingStatus status) {
        this.status = status;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_EVIDENCE) {
            return value;
        }
        return value.substring(0, MAX_EVIDENCE);
    }

    public Long getId() {
        return id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTitle() {
        return title;
    }

    public Severity getSeverity() {
        return severity;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public String getOwaspCategory() {
        return owaspCategory;
    }

    public String getAttackTactic() {
        return attackTactic;
    }

    public String getAttackTechnique() {
        return attackTechnique;
    }

    public String getActor() {
        return actor;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Long getAffectedAccountId() {
        return affectedAccountId;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getIntel() {
        return intel;
    }
}
