package com.gattopiccolo.ledger.threat.enrich;

/**
 * Threat-intelligence verdict for a source address.
 *
 * @param label feed or reason that produced the verdict, may be null
 */
public record IpReputation(String ip, Verdict verdict, String label) {

    public enum Verdict {
        /** No information. */
        UNKNOWN,
        /** Loopback or private range, not routable from the internet. */
        INTERNAL,
        /** Listed in a threat-intel feed. */
        MALICIOUS
    }

    public static IpReputation unknown(String ip) {
        return new IpReputation(ip, Verdict.UNKNOWN, null);
    }

    /** Short human-readable form stored with the finding. */
    public String summary() {
        return label == null ? verdict.name() : verdict.name() + ": " + label;
    }
}
