package com.gattopiccolo.ledger.threat.enrich;

/**
 * Source of IP reputation. A production implementation would call a feed such
 * as AbuseIPDB or an internal TI platform (with caching and timeouts); the
 * prototype ships a static, configuration-driven one.
 */
public interface ThreatIntelProvider {

    IpReputation lookup(String ip);
}
