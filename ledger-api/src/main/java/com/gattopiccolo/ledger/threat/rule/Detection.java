package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.Severity;

import java.time.Instant;

/**
 * Raw output of a {@link DetectionRule}: one signal that something suspicious
 * happened. Many detections can collapse into a single finding (see the
 * fingerprint), which is how the prototype fights alert fatigue.
 */
public record Detection(String ruleId,
                        String title,
                        Severity severity,
                        String owaspCategory,
                        String attackTactic,
                        String attackTechnique,
                        String actor,
                        String sourceIp,
                        Long affectedAccountId,
                        String evidence,
                        Instant at) {

    /**
     * Deduplication key: the same rule firing for the same actor from the same
     * address is the same story, however many times it repeats.
     */
    public String fingerprint() {
        return ruleId + ":" + actor + ":" + (sourceIp == null ? "-" : sourceIp);
    }
}
