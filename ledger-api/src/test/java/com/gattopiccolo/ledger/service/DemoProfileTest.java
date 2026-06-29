package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.bootstrap.DemoDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("demo")
class DemoProfileTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void demoProfileUsesNoOpExternalLoggingClient() {
        assertThat(externalLoggingClient).isInstanceOf(NoOpExternalLoggingClient.class);
        assertThat(context.getBeanNamesForType(HttpExternalLoggingClient.class)).isEmpty();
    }

    @Test
    void demoProfileEnablesDemoDataInitializer() {
        assertThat(context.getBeanNamesForType(DemoDataInitializer.class)).hasSize(1);
    }
}
