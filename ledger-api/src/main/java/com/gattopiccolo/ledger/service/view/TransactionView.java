package com.gattopiccolo.ledger.service.view;

import com.gattopiccolo.ledger.domain.AccountTransaction;
import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        CurrencyCode currency,
        Long counterpartyAccountId,
        String description,
        Instant createdAt) {

    public static TransactionView of(AccountTransaction t) {
        CurrencyCode c = t.getCurrency();
        return new TransactionView(
                t.getId(),
                t.getAccount().getId(),
                t.getType(),
                c.round(t.getAmount()),
                c.round(t.getBalanceAfter()),
                c,
                t.getCounterpartyAccountId(),
                t.getDescription(),
                t.getCreatedAt());
    }
}
