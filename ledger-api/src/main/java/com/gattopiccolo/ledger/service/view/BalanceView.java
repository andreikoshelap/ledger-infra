package com.gattopiccolo.ledger.service.view;

import com.gattopiccolo.ledger.domain.Account;
import com.gattopiccolo.ledger.domain.CurrencyCode;

public record BalanceView(Long id, CurrencyCode currency, String balance) {

    public static BalanceView of(Account account) {
        CurrencyCode c = account.getCurrency();
        return new BalanceView(
                account.getId(), c,
                c.round(account.getBalance()).toPlainString());  // rendered string
    }
}
