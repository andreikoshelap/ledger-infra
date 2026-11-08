package com.gattopiccolo.ledger.threat.enrich;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Threat intel backed by a fixed map of known-bad addresses (ip -> label).
 * Never performs DNS lookups: only literal IP addresses are interpreted.
 */
public final class StaticThreatIntelProvider implements ThreatIntelProvider {

    private final Map<String, String> maliciousIps;

    public StaticThreatIntelProvider(Map<String, String> maliciousIps) {
        this.maliciousIps = Map.copyOf(maliciousIps);
    }

    @Override
    public IpReputation lookup(String ip) {
        if (ip == null || ip.isBlank()) {
            return IpReputation.unknown(ip);
        }
        String label = maliciousIps.get(ip);
        if (label != null) {
            return new IpReputation(ip, IpReputation.Verdict.MALICIOUS, label);
        }
        if (isInternal(ip)) {
            return new IpReputation(ip, IpReputation.Verdict.INTERNAL, null);
        }
        return IpReputation.unknown(ip);
    }

    private static boolean isInternal(String ip) {
        boolean literal = ip.contains(":") || ip.matches("[0-9.]+");
        if (!literal) {
            return false; // a hostname would trigger DNS; refuse
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
