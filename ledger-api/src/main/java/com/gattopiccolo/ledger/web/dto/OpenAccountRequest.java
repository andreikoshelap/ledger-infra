package com.gattopiccolo.ledger.web.dto;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@NotNull CurrencyCode currency) {}