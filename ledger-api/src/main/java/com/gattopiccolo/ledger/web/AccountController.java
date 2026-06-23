package com.gattopiccolo.ledger.web;

import com.gattopiccolo.ledger.service.AccountService;
import com.gattopiccolo.ledger.service.view.BalanceView;
import com.gattopiccolo.ledger.service.view.ExchangeResult;
import com.gattopiccolo.ledger.service.view.TransactionPage;
import com.gattopiccolo.ledger.service.view.TransactionView;
import com.gattopiccolo.ledger.web.dto.AmountRequest;
import com.gattopiccolo.ledger.web.dto.ExchangeRequest;
import com.gattopiccolo.ledger.web.dto.OpenAccountRequest;
import com.gattopiccolo.ledger.web.dto.QuoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Read API consumed by the Angular front-end. Effective paths are prefixed with
 * {@code /api} by {@link com.gattopiccolo.ledger.config.WebMvcConfig}. The caller's
 * identity arrives in the {@code X-User-Id} header (set by the web auth interceptor).
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<BalanceView> listAccounts(@RequestHeader("X-User-Id") Long userId) {
        return accountService.listAccounts(userId);
    }

    @GetMapping("/{id}")
    public BalanceView getAccount(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/{id}/balance")
    public BalanceView getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/{id}/transactions")
    public TransactionPage getHistory(@PathVariable Long id,
                                      @RequestParam(required = false) Long cursor,
                                      @RequestParam(required = false) Integer limit) {
        return accountService.history(id, cursor, limit);
    }

    @GetMapping("/{id}/transactions/{entryId}")
    public TransactionView getTransaction(@PathVariable Long id, @PathVariable Long entryId) {
        return accountService.getTransaction(id, entryId);
    }

    @PostMapping
    ResponseEntity<BalanceView> open(
            @RequestHeader("X-User-Id") long userId,
            @Valid @RequestBody OpenAccountRequest body,
            UriComponentsBuilder uri) {

        BalanceView created = accountService.open(userId, body.currency());
        URI location = uri.path("/api/accounts/{id}").build(created.id());
        return ResponseEntity.created(location).body(created); // 201 + Location
    }

    @PostMapping("/{id}/deposit")
    public TransactionView deposit(@PathVariable Long id,
                                   @Valid @RequestBody AmountRequest body) {
        return accountService.deposit(id, body.amount());
    }

    @PostMapping("/{id}/debit")
    public TransactionView debit(@PathVariable Long id,
                                 @Valid @RequestBody AmountRequest body) {
        return accountService.debit(id, body.amount());
    }

    @PostMapping("/exchange")
    public ExchangeResult exchange(@RequestHeader("X-User-Id") Long userId,
                                   @Valid @RequestBody ExchangeRequest body) {
        return accountService.exchange(userId, body.fromAccountId(), body.toAccountId(), body.amount());
    }

    @GetMapping("/quote")
    public QuoteResponse quote(@RequestParam Long from,
                               @RequestParam Long to,
                               @RequestParam BigDecimal amount) {
        return accountService.quote(from, to, amount);
    }
}
