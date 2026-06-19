package com.gattopiccolo.ledger.web;

import com.gattopiccolo.ledger.service.AccountService;
import com.gattopiccolo.ledger.service.view.BalanceView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/balance")
    public BalanceView getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }
}
