package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the overdraft guard holds under concurrency: 15 threads each try to
 * debit 10 from a balance of 100. Exactly 10 must succeed, 5 must fail with
 * insufficient funds, and the final balance must be exactly 0 (never negative).
 * This only passes because the debit path takes a pessimistic row lock.
 */
@SpringBootTest
class ConcurrentDebitTest {

    @Autowired
    private AccountService service;

    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void concurrentDebitsNeverOverdraw() throws Exception {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        int threads = 15;
        AtomicInteger succeeded;
        AtomicInteger insufficient;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            succeeded = new AtomicInteger();
            insufficient = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        start.await();
                        service.debit(id, new BigDecimal("10.00"));
                        succeeded.incrementAndGet();
                    } catch (InsufficientFundsException e) {
                        insufficient.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            start.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
            pool.shutdown();
        }

        assertEquals(10, succeeded.get());
        assertEquals(5, insufficient.get());
        assertEquals("0.00", service.getBalance(id).balance());
    }
}
