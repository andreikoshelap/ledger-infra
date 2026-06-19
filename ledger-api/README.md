# Ledger — core domain (Part 1)

Self-contained Spring Boot microservice for bank-account handling: deposits,
debits, balance, currency exchange and transaction history, persisted in an
SQL database. This stage delivers the **domain core + service layer + tests**.
REST controllers and the global error handler are the thin layer on top and
come next.

## Stack
- Java 21, Spring Boot 4.1.x, Gradle (Kotlin DSL)
- Spring Data JPA + H2 (in-memory)
- Schema generated from entities for zero-config boot; a Flyway migration with
  the same schema is included for the production path.

## Run
The Gradle wrapper jar is not committed; generate it once (or just open the
project in IntelliJ, which does it for you):
```bash
gradle wrapper --gradle-version 8.10
```
Then:
```bash
./gradlew test          # full test suite (incl. concurrency + external-call gating)
./gradlew bootRun       # starts on :8080, H2 console at /h2-console
./gradlew bootRun --args='--spring.profiles.active=demo'   # seed sample data
```

## Design decisions (the things worth defending)

**Money.** All amounts are `BigDecimal`; no `double` anywhere. Each currency
declares its scale — EUR/USD/SEK/GBP use 2 decimals, **VND uses 0**. User input
is validated (`requireValidScale`) and *rejected* rather than silently rounded
if it carries more precision than the currency allows. Conversion rounds with
`HALF_EVEN` (banker's rounding).

**No overdraft under concurrency.** The funds check and balance update happen
inside a pessimistic row lock (`SELECT ... FOR UPDATE`, via
`findByIdForUpdate`). A DB `CHECK (balance >= 0)` constraint is the last line of
defence. `ConcurrentDebitTest` fires 15 simultaneous debits at a balance that
covers only 10 and asserts exactly 10 succeed and the balance lands on 0.

**External call before debit, outside the transaction.** The required
pre-debit call to the external system (`httpstat.us`) runs in `debit()` *before*
the DB transaction opens, so a slow or failing external system never blocks
while a row lock is held. A non-2xx/timeout aborts the debit before any DB work.
This is why `debit()`/`exchange()` use a `TransactionTemplate` for the locked
section instead of method-level `@Transactional`.

**Exchange = atomic two-account move.** Debit source + credit converted target
in one transaction, locking both accounts in ascending id order to avoid
deadlocks between mirror-image exchanges.

**History.** Cursor (seek) pagination on the monotonic transaction id —
stable under inserts, ideal for the front-end infinite scroll. Each entry
stores `balanceAfter`, so the account-balance time series for the chart is
served straight from the ledger with no recomputation.

**Auth is out of scope** per the assignment. `userId` is passed explicitly;
the boundary where real authentication would plug in is the service entry point.

## Layout
```
domain/      Account, AccountTransaction, CurrencyCode, TransactionType
money/       ExchangeRateProvider (fixed rates via EUR pivot)
repository/  JPA repositories (incl. pessimistic-lock query)
service/     AccountService, ExternalLoggingClient, view records
config/      RestClient with timeouts
```

## Next (Part 1 cont.)
REST controllers (`/accounts`, `/accounts/{id}/transactions`, exchange),
`@RestControllerAdvice` returning `application/problem+json`, request validation.
