package com.gattopiccolo.ledger.threat.enrich;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticThreatIntelProviderTest {

    private final ThreatIntelProvider intel =
            new StaticThreatIntelProvider(Map.of("203.0.113.66", "sample-feed"));

    @Test
    void listedAddressIsMalicious() {
        var rep = intel.lookup("203.0.113.66");
        assertEquals(IpReputation.Verdict.MALICIOUS, rep.verdict());
        assertEquals("MALICIOUS: sample-feed", rep.summary());
    }

    @Test
    void privateAndLoopbackAddressesAreInternal() {
        assertEquals(IpReputation.Verdict.INTERNAL, intel.lookup("127.0.0.1").verdict());
        assertEquals(IpReputation.Verdict.INTERNAL, intel.lookup("10.1.2.3").verdict());
        assertEquals(IpReputation.Verdict.INTERNAL, intel.lookup("192.168.0.9").verdict());
    }

    @Test
    void publicUnlistedAddressIsUnknown() {
        assertEquals(IpReputation.Verdict.UNKNOWN, intel.lookup("198.51.100.7").verdict());
    }

    @Test
    void nullBlankAndHostnamesAreUnknownWithoutDnsLookup() {
        assertEquals(IpReputation.Verdict.UNKNOWN, intel.lookup(null).verdict());
        assertEquals(IpReputation.Verdict.UNKNOWN, intel.lookup(" ").verdict());
        assertEquals(IpReputation.Verdict.UNKNOWN, intel.lookup("localhost").verdict());
    }
}
