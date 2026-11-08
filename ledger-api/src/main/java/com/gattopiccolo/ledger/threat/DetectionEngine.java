package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.threat.rule.DetectionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pipeline entry point: buffer the event, run every rule, hand detections to
 * the correlation layer. Processing is serialised so deduplication is
 * race-free on a single instance; scaling out would need a shared store or
 * keyed partitioning (for example by actor on a Kafka topic).
 */
@Component
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    private final EventWindow window;
    private final List<DetectionRule> rules;
    private final FindingService findings;

    public DetectionEngine(EventWindow window, List<DetectionRule> rules, FindingService findings) {
        this.window = window;
        this.rules = rules;
        this.findings = findings;
    }

    public synchronized void onEvent(SecurityEvent event) {
        window.add(event);
        for (DetectionRule rule : rules) {
            try {
                rule.evaluate(event, window).ifPresent(findings::raise);
            } catch (RuntimeException ex) {
                // A broken rule must never take the others, or the request, down.
                log.warn("Detection rule {} failed", rule.id(), ex);
            }
        }
    }

    public EventWindow window() {
        return window;
    }
}
