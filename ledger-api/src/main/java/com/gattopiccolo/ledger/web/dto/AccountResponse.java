package com.gattopiccolo.ledger.web.dto;

import com.gattopiccolo.ledger.domain.Account;
import com.gattopiccolo.ledger.domain.CurrencyCode;

import java.math.RoundingMode;

public record AccountResponse(long id, CurrencyCode currency, String balance) {
    public static AccountResponse from(Account a) {
        String balance = a.getBalance()
                .setScale(a.getCurrency().displayScale(), RoundingMode.HALF_EVEN)
                .toPlainString();
        return new AccountResponse(a.getId(), a.getCurrency(), balance);
    }
}