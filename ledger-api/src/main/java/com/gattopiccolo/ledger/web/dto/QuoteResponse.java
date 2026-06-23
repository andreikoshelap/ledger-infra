package com.gattopiccolo.ledger.web.dto;

import com.gattopiccolo.ledger.domain.CurrencyCode;

public record QuoteResponse(String amount, String converted,
                            CurrencyCode from, CurrencyCode to) { }