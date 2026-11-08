package com.gattopiccolo.ledger.threat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Tunables of the threat-observability module ({@code ledger.security.*}).
 * Every value has a default, so the module works with no configuration at all.
 *
 * @param enabled                  master switch for the request sensor
 * @param window                   sliding window used by the aggregating rules
 * @param enumerationThreshold     distinct probed account ids that trigger LEDGER-002
 * @param debitVelocityThreshold   successful debits per window that trigger LEDGER-003
 * @param clientErrorThreshold     4xx responses per window that trigger LEDGER-004
 * @param suppression              how long an active finding keeps absorbing duplicates
 * @param trustForwardedFor        use the first {@code X-Forwarded-For} hop as source IP;
 *                                 enable only behind a proxy you control, the header is spoofable
 * @param maliciousIps             static threat-intel feed: ip -> label
 */
@ConfigurationProperties(prefix = "ledger.security")
public record SecurityProperties(Boolean enabled,
                                 Duration window,
                                 Integer enumerationThreshold,
                                 Integer debitVelocityThreshold,
                                 Integer clientErrorThreshold,
                                 Duration suppression,
                                 Boolean trustForwardedFor,
                                 Map<String, String> maliciousIps) {

    public SecurityProperties {
        enabled = enabled == null || enabled;
        window = window == null ? Duration.ofMinutes(5) : window;
        enumerationThreshold = enumerationThreshold == null ? 5 : enumerationThreshold;
        debitVelocityThreshold = debitVelocityThreshold == null ? 10 : debitVelocityThreshold;
        clientErrorThreshold = clientErrorThreshold == null ? 10 : clientErrorThreshold;
        suppression = suppression == null ? Duration.ofHours(1) : suppression;
        trustForwardedFor = trustForwardedFor != null && trustForwardedFor;
        maliciousIps = maliciousIps == null ? Map.of() : Map.copyOf(maliciousIps);
    }
}
