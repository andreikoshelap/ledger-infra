package com.gattopiccolo.ledger.threat;

/** Lifecycle of a finding. Only active findings absorb new duplicate detections. */
public enum FindingStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED;

    public boolean isActive() {
        return this != RESOLVED;
    }
}
