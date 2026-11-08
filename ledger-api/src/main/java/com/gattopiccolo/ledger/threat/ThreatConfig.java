package com.gattopiccolo.ledger.threat;

import com.gattopiccolo.ledger.threat.enrich.StaticThreatIntelProvider;
import com.gattopiccolo.ledger.threat.enrich.ThreatIntelProvider;
import com.gattopiccolo.ledger.threat.rule.AccountEnumerationRule;
import com.gattopiccolo.ledger.threat.rule.ClientErrorProbingRule;
import com.gattopiccolo.ledger.threat.rule.DebitVelocityRule;
import com.gattopiccolo.ledger.threat.rule.DetectionRule;
import com.gattopiccolo.ledger.threat.rule.ForeignAccountAccessRule;
import com.gattopiccolo.ledger.threat.vuln.VulnerabilityPrioritizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the plain-Java threat-observability building blocks into the application. */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class ThreatConfig {

    private static final int EVENT_BUFFER_SIZE = 2000;

    @Bean
    EventWindow eventWindow() {
        return new EventWindow(EVENT_BUFFER_SIZE);
    }

    @Bean
    DetectionRule foreignAccountAccessRule() {
        return new ForeignAccountAccessRule();
    }

    @Bean
    DetectionRule accountEnumerationRule(SecurityProperties props) {
        return new AccountEnumerationRule(props.window(), props.enumerationThreshold());
    }

    @Bean
    DetectionRule debitVelocityRule(SecurityProperties props) {
        return new DebitVelocityRule(props.window(), props.debitVelocityThreshold());
    }

    @Bean
    DetectionRule clientErrorProbingRule(SecurityProperties props) {
        return new ClientErrorProbingRule(props.window(), props.clientErrorThreshold());
    }

    @Bean
    ThreatIntelProvider threatIntelProvider(SecurityProperties props) {
        return new StaticThreatIntelProvider(props.maliciousIps());
    }

    @Bean
    VulnerabilityPrioritizer vulnerabilityPrioritizer() {
        return new VulnerabilityPrioritizer();
    }
}
