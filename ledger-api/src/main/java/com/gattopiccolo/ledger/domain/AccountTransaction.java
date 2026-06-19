package com.gattopiccolo.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * An immutable ledger entry. {@code balanceAfter} is captured at write time so
 * the account-balance time series (for the front-end chart) and the running
 * history can both be served straight from this table without recomputation.
 */
@Entity
@Table(name = "account_transaction")
@Check(constraints = "amount > 0")
public class AccountTransaction {

    private static final int STORAGE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 38, scale = STORAGE_SCALE)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 38, scale = STORAGE_SCALE)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "counterparty_account_id")
    private Long counterpartyAccountId;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountTransaction() {
        // for JPA
    }

    /**
     * Records an entry. Call AFTER the account balance has been mutated so that
     * {@code balanceAfter} reflects the post-transaction state.
     */
    public static AccountTransaction record(Account account,
                                            TransactionType type,
                                            BigDecimal amount,
                                            Long counterpartyAccountId,
                                            String description) {
        AccountTransaction t = new AccountTransaction();
        t.account = account;
        t.type = type;
        t.amount = amount.setScale(STORAGE_SCALE, RoundingMode.HALF_EVEN);
        t.balanceAfter = account.getBalance();
        t.currency = account.getCurrency();
        t.counterpartyAccountId = counterpartyAccountId;
        t.description = description;
        return t;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public Long getCounterpartyAccountId() {
        return counterpartyAccountId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
