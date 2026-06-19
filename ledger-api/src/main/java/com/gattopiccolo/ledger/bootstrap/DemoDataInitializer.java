package com.gattopiccolo.ledger.bootstrap;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a demo user with two accounts and some history. Enabled only under the
 * "demo" profile:  ./gradlew bootRun --args='--spring.profiles.active=demo'
 * Note: debit/exchange here perform a real call to the configured external
 * logging endpoint (httpstat.us), so network access is required for the demo.
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final Long DEMO_USER = 1L;

    private final AccountService service;

    public DemoDataInitializer(AccountService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        Long eur = service.openAccount(DEMO_USER, CurrencyCode.EUR);
        Long usd = service.openAccount(DEMO_USER, CurrencyCode.USD);

        service.deposit(eur, new BigDecimal("1000.00"));
        service.deposit(usd, new BigDecimal("500.00"));
        service.debit(eur, new BigDecimal("120.50"));
        service.exchange(eur, usd, new BigDecimal("100.00"));

        log.info("Demo data ready: user={}, EUR account={}, USD account={}", DEMO_USER, eur, usd);
    }
}
