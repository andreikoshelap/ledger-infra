package com.gattopiccolo.ledger.threat;

import java.time.Instant;

/**
 * One normalised security-relevant observation, analogous to a log line or a
 * security event ingested into a SIEM / data lakehouse.
 *
 * @param at          event time (rules treat the latest event time as "now")
 * @param actor       caller identity as claimed by the {@code X-User-Id} header, or {@code anonymous}
 * @param sourceIp    network address of the caller, may be null
 * @param accountId   account addressed by the request path, may be null
 * @param ownerUserId real owner of that account, null when the account does not exist
 */
public record SecurityEvent(Instant at,
                            String actor,
                            String sourceIp,
                            String method,
                            String path,
                            int status,
                            Long accountId,
                            Long ownerUserId,
                            EventType type) {

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    public boolean isMutation() {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
