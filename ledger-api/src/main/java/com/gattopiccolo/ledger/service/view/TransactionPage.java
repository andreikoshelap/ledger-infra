package com.gattopiccolo.ledger.service.view;

import java.util.List;

/**
 * A slice of transaction history. {@code nextCursor} is the id to pass back to
 * fetch the following page, or {@code null} when there are no more rows.
 */
public record TransactionPage(List<TransactionView> items, Long nextCursor) {
}
