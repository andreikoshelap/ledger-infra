package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.bootstrap.DemoDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("postman")
class PostmanProfileTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ExternalLoggingClient externalLoggingClient;

    @Autowired
    private Environment environment;

    @Test
    void postmanProfileUsesHttpExternalLoggingClient() {
        assertThat(externalLoggingClient).isInstanceOf(HttpExternalLoggingClient.class);
        assertThat(context.getBeanNamesForType(NoOpExternalLoggingClient.class)).isEmpty();
    }

    @Test
    void postmanProfileDoesNotEnableDemoDataInitializer() {
        assertThat(context.getBeanNamesForType(DemoDataInitializer.class)).isEmpty();
    }

    @Test
    void postmanProfileUsesPostmanEchoEndpoint() {
        assertThat(environment.getProperty("ledger.external-logging.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("ledger.external-logging.base-url")).isEqualTo("https://postman-echo.com");
        assertThat(environment.getProperty("ledger.external-logging.path")).isEqualTo("/status/200");
    }
}
