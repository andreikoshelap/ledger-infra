package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.*;
import com.gattopiccolo.ledger.exception.AccountNotFoundException;
import com.gattopiccolo.ledger.exception.InvalidAmountException;
import com.gattopiccolo.ledger.exception.TransactionNotFoundException;
import com.gattopiccolo.ledger.money.ExchangeRateProvider;
import com.gattopiccolo.ledger.repository.AccountRepository;
import com.gattopiccolo.ledger.repository.AccountTransactionRepository;
import com.gattopiccolo.ledger.service.view.BalanceView;
import com.gattopiccolo.ledger.service.view.ExchangeResult;
import com.gattopiccolo.ledger.service.view.TransactionPage;
import com.gattopiccolo.ledger.service.view.TransactionView;
import com.gattopiccolo.ledger.web.dto.QuoteResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AccountRepository accounts;
    private final AccountTransactionRepository transactions;
    private final ExchangeRateProvider rates;
    private final ExternalLoggingClient externalLoggingClient;
    private final TransactionTemplate tx;

    public AccountService(AccountRepository accounts,
                          AccountTransactionRepository transactions,
                          ExchangeRateProvider rates,
                          ExternalLoggingClient externalLoggingClient,
                          PlatformTransactionManager txManager) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.rates = rates;
        this.externalLoggingClient = externalLoggingClient;
        this.tx = new TransactionTemplate(txManager);
    }

    // ----- account & reads -------------------------------------------------

    @Transactional
    public Long openAccount(Long userId, CurrencyCode currency) {
        return accounts.save(Account.open(userId, currency, AccountType.CUSTOMER)).getId();
    }

    @Transactional(readOnly = true)
    public List<BalanceView> listAccounts(Long userId) {
        return accounts.findByUserIdOrderByIdAsc(userId).stream().map(BalanceView::of).toList();
    }

    @Transactional(readOnly = true)
    public BalanceView getBalance(Long accountId) {
        return BalanceView.of(load(accountId));
    }

    @Transactional(readOnly = true)
    public TransactionPage history(Long accountId, Long cursor, Integer limit) {
        load(accountId); // 404 early if the account does not exist
        int size = clampPageSize(limit);
        Pageable page = PageRequest.of(0, size);

        List<AccountTransaction> rows = (cursor == null)
                ? transactions.findByAccountIdOrderByIdDesc(accountId, page)
                : transactions.findByAccountIdAndIdLessThanOrderByIdDesc(accountId, cursor, page);

        List<TransactionView> items = rows.stream().map(TransactionView::of).toList();
        Long next = (items.size() == size) ? items.get(items.size() - 1).id() : null;
        return new TransactionPage(items, next);
    }

    @Transactional(readOnly = true)
    public TransactionView getTransaction(Long accountId, Long entryId) {
        load(accountId); // 404 early if the account does not exist
        return transactions.findByIdAndAccountId(entryId, accountId)
                .map(TransactionView::of)
                .orElseThrow(() -> new TransactionNotFoundException(entryId));
    }

    // ----- money mutations -------------------------------------------------

    @Transactional
    public TransactionView deposit(Long accountId, BigDecimal rawAmount) {
        requirePositive(rawAmount);
        Account account = lock(accountId);
        BigDecimal amount = account.getCurrency().requireValidScale(rawAmount);
        account.credit(amount);
        AccountTransaction t = transactions.save(
                AccountTransaction.record(account, TransactionType.DEPOSIT, amount, null, "Deposit"));
        return TransactionView.of(t);
    }

    /**
     * Debit is deliberately NOT annotated @Transactional. The external logging
     * call must complete BEFORE the DB transaction opens, otherwise we would hold
     * a pessimistic row lock for the duration of a network round-trip. The actual
     * balance mutation then runs in its own short, locked transaction.
     */
    public TransactionView debit(Long accountId, BigDecimal rawAmount) {
        requirePositive(rawAmount);
        externalLoggingClient.logBeforeDebit(accountId, rawAmount); // may throw -> aborts before any DB work

        return tx.execute(status -> {
            Account account = lock(accountId);
            BigDecimal amount = account.getCurrency().requireValidScale(rawAmount);
            account.debit(amount); // throws InsufficientFundsException if balance is too low
            AccountTransaction t = transactions.save(
                    AccountTransaction.record(account, TransactionType.DEBIT, amount, null, "Debit"));
            return TransactionView.of(t);
        });
    }

    /**
     * Currency exchange = debit one account, credit another (possibly different
     * currency) atomically. Accounts are locked in a deterministic id order to
     * avoid deadlocks between two concurrent, mirror-image exchanges. The external
     * logging call runs first, outside the transaction (the source is debited).
     */
    public ExchangeResult exchange(Long userId, Long fromAccountId, Long toAccountId, BigDecimal rawAmount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidAmountException("Source and target accounts must differ");
        }
        requirePositive(rawAmount);
        externalLoggingClient.logBeforeDebit(fromAccountId, rawAmount);

        return tx.execute(status -> {
            long firstId = Math.min(fromAccountId, toAccountId);
            long secondId = Math.max(fromAccountId, toAccountId);
            Account first = lock(firstId, userId);
            Account second = lock(secondId, userId);
            Account from = fromAccountId == firstId ? first : second;
            Account to = fromAccountId == firstId ? second : first;

            BigDecimal debitAmount = from.getCurrency().requireValidScale(rawAmount);
            BigDecimal creditAmount = rates.convert(debitAmount, from.getCurrency(), to.getCurrency());

            from.debit(debitAmount);
            AccountTransaction out = transactions.save(AccountTransaction.record(
                    from, TransactionType.EXCHANGE_OUT, debitAmount, to.getId(),
                    "Exchange to " + to.getCurrency()));

            to.credit(creditAmount);
            AccountTransaction in = transactions.save(AccountTransaction.record(
                    to, TransactionType.EXCHANGE_IN, creditAmount, from.getId(),
                    "Exchange from " + from.getCurrency()));

            return new ExchangeResult(TransactionView.of(out), TransactionView.of(in));
        });
    }

    @Transactional(readOnly = true)
    public QuoteResponse quote(Long fromAccountId, Long toAccountId, BigDecimal rawAmount) {
        requirePositive(rawAmount);
        Account from = load(fromAccountId);
        Account to = load(toAccountId);
        BigDecimal amount = from.getCurrency().requireValidScale(rawAmount);
        BigDecimal converted = rates.convert(amount, from.getCurrency(), to.getCurrency());
        return new QuoteResponse(
                from.getCurrency().round(amount).toPlainString(),
                to.getCurrency().round(converted).toPlainString(),
                from.getCurrency(), to.getCurrency());
    }

    // ----- helpers ---------------------------------------------------------

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Account lock(Long id, Long userId) {
        return accounts.findByIdForUpdateAndUserId(id, userId)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Account load(Long id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }

    private static int clampPageSize(Integer limit) {
        if (limit == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
    }

    @Transactional
    public BalanceView open(long userId, CurrencyCode currency) {
        Account saved = accounts.save(Account.open(userId, currency));
        return BalanceView.of(saved);
    }
}
