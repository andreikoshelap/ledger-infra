package com.gattopiccolo.ledger.web;

import com.gattopiccolo.ledger.service.AccountService;
import com.gattopiccolo.ledger.service.view.BalanceView;
import com.gattopiccolo.ledger.service.view.TransactionPage;
import com.gattopiccolo.ledger.service.view.TransactionView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
