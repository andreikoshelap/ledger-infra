package com.gattopiccolo.ledger.domain;

import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * An account holds a balance in exactly one currency. Balance mutations are
 * encapsulated here (credit/debit) so the funds invariant lives with the data.
 * The DB-level check constraint is the final guard against a negative balance.
 */
@Entity
@Table(name = "account")
@Check(constraints = "balance >= 0")
public class Account {

    /** Internal working scale for stored monetary values. */
    private static final int STORAGE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false, precision = 38, scale = STORAGE_SCALE)
    private BigDecimal balance;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated
    @Column(name = "type")
    private AccountType type;

    protected Account() {
        // for JPA
    }

    private Account(Long userId, CurrencyCode currency, AccountType type) {
        this.userId = userId;
        this.currency = currency;
        this.type = type;
        this.balance = BigDecimal.ZERO.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
    }

    public static Account open(Long userId, CurrencyCode currency) {
        return open(userId, currency, AccountType.CUSTOMER);
    }

    public static Account open(Long userId, CurrencyCode currency, AccountType type) {
        return new Account(userId, currency, type);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(STORAGE_SCALE, RoundingMode.HALF_EVEN);
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id);
        }
        this.balance = this.balance.subtract(amount).setScale(STORAGE_SCALE, RoundingMode.HALF_EVEN);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AccountType getType() {
        return type;
    }
}
