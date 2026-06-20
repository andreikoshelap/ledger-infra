package com.gattopiccolo.ledger.service.view;

import com.gattopiccolo.ledger.domain.Account;
import com.gattopiccolo.ledger.domain.CurrencyCode;

import java.math.BigDecimal;

public record BalanceView(Long id, CurrencyCode currency, BigDecimal balance) {

    public static BalanceView of(Account account) {
        return new BalanceView(
                account.getId(),
                account.getCurrency(),
                account.getCurrency().round(account.getBalance()));
    }
}
