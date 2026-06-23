# Ledger Infra

Monorepo with two applications:

- `ledger-api` - Spring Boot backend
- `ledger-web` - Angular frontend

The system supports:

- opening accounts in a single currency
- deposits and debits
- balance reads
- exchange between two accounts
- transaction history with cursor pagination

## UI Screenshots

### Accounts

![Accounts page](docs/images/accounts-page.png)

### Account

![Account page](docs/images/account-page.png)

### Transaction

![Transaction page](docs/images/transaction-page.png)

## Architecture

This implementation uses a stored-balance, single-entry ledger model with a
transaction journal.

In practical terms:

- one account has one currency
- the current balance is stored on the account row
- every mutation also writes a journal entry
- exchange is the only operation that touches two accounts atomically

For this scope, that choice is deliberate.

## Why Not Double-Entry

For this model, double-entry would be over-engineering.

The system does not model a full general ledger. It models customer accounts
with stored balances and an auditable transaction stream. There are no broader
cross-account invariants here beyond exchange, and exchange is already handled
atomically inside a single database transaction.

I understand the trade-off:

- double-entry gives conservation of money and a built-in zero-sum invariant
- derived balance gives full auditability from journal replay
- both come with more operational and implementation complexity

For homework scope, stored single-entry is simpler and sufficient.

For a real bank-grade ledger with settlement flows between many accounts,
stronger reconciliation guarantees, and regulatory audit requirements, I would
choose double-entry with derived balances.

## Backend Design

Key decisions in `ledger-api`:

- money uses `BigDecimal`, never `double`
- each currency enforces its own scale
- invalid precision is rejected instead of silently rounded
- debit and exchange perform the external pre-debit call before entering the
  locked transaction
- account mutations use pessimistic locking to prevent concurrent overdraft
- `CHECK (balance >= 0)` is the final database guard
- exchange locks both accounts in deterministic id order to avoid deadlocks
- history uses cursor pagination on transaction id
- each journal row stores `balanceAfter`, so chart/history reads are cheap

## Profiles

Backend runtime is split by profile:

- default local run: H2
- Docker run: PostgreSQL + Flyway

Files:

- `ledger-api/src/main/resources/application.yml`
- `ledger-api/src/main/resources/application-docker.yml`

## Run Locally

Backend:

```bash
cd ledger-api
./gradlew test
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=demo'
```

Frontend:

```bash
cd ledger-web
npm install
ng serve --proxy-config proxy.conf.json
```
Locally frontend is exposed on `http://localhost:4200`.

## Run With Docker

From repo root:

```bash
docker compose down -v
docker compose up --build -d
```

Frontend is exposed on `http://localhost:8080`.

## Repo Layout

```text
ledger-api/
  src/main/java/com/gattopiccolo/ledger/
  src/main/resources/
ledger-web/
  src/app/
docker-compose.yml
```
