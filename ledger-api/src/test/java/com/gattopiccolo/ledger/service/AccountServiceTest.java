package com.gattopiccolo.ledger.service;

import com.gattopiccolo.ledger.domain.CurrencyCode;
import com.gattopiccolo.ledger.domain.TransactionType;
import com.gattopiccolo.ledger.exception.AccountNotFoundException;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import com.gattopiccolo.ledger.exception.InvalidAmountException;
import com.gattopiccolo.ledger.exception.TransactionNotFoundException;
import com.gattopiccolo.ledger.service.view.BalanceView;
import com.gattopiccolo.ledger.service.view.TransactionView;
import com.gattopiccolo.ledger.service.view.TransactionPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService service;

    // No-op so these tests never touch the real external endpoint.
    @MockitoBean
    private ExternalLoggingClient externalLoggingClient;

    @Test
    void openAccountReturnsPersistedAccountId() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        assertNotNull(id);
        assertEquals(id, service.getBalance(id).id());
        assertEquals(CurrencyCode.EUR, service.getBalance(id).currency());
        assertEquals("0.00", service.getBalance(id).balance());
    }

    @Test
    void openReturnsBalanceViewForNewAccount() {
        BalanceView opened = service.open(7L, CurrencyCode.USD);

        assertNotNull(opened.id());
        assertEquals(CurrencyCode.USD, opened.currency());
        assertEquals("0.00", opened.balance());
    }

    @Test
    void listAccountsReturnsOnlyRequestedUsersAccountsInIdOrder() {
        Long first = service.openAccount(10L, CurrencyCode.EUR);
        service.openAccount(11L, CurrencyCode.GBP);
        Long second = service.openAccount(10L, CurrencyCode.USD);

        List<BalanceView> accounts = service.listAccounts(10L);

        assertEquals(2, accounts.size());
        assertEquals(List.of(first, second), accounts.stream().map(BalanceView::id).toList());
        assertEquals(List.of(CurrencyCode.EUR, CurrencyCode.USD),
                accounts.stream().map(BalanceView::currency).toList());
    }

    @Test
    void getBalanceReturnsCurrentBalance() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        BalanceView balance = service.getBalance(id);

        assertEquals(id, balance.id());
        assertEquals(CurrencyCode.EUR, balance.currency());
        assertEquals("100.00", balance.balance());
    }

    @Test
    void getBalanceForMissingAccountThrows() {
        assertThrows(AccountNotFoundException.class, () -> service.getBalance(999_999L));
    }

    @Test
    void depositCreatesDepositTransactionAndIncreasesBalance() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        TransactionView transaction = service.deposit(id, new BigDecimal("100.00"));

        assertEquals(TransactionType.DEPOSIT, transaction.type());
        assertEquals(id, transaction.accountId());
        assertEquals("100.00", transaction.amount());
        assertEquals("100.00", transaction.balanceAfter());
        assertEquals("Deposit", transaction.description());
        assertEquals("100.00", service.getBalance(id).balance());
    }

    @Test
    void depositRejectsNonPositiveAmounts() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        assertThrows(InvalidAmountException.class, () -> service.deposit(id, BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> service.deposit(id, new BigDecimal("-1.00")));
    }

    @Test
    void depositForMissingAccountThrows() {
        assertThrows(AccountNotFoundException.class,
                () -> service.deposit(999_999L, new BigDecimal("10.00")));
    }

    @Test
    void debitCreatesDebitTransactionAndReducesBalance() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("100.00"));

        TransactionView transaction = service.debit(id, new BigDecimal("30.00"));

        assertEquals(TransactionType.DEBIT, transaction.type());
        assertEquals("-30.00", transaction.amount());
        assertEquals("70.00", transaction.balanceAfter());
        assertEquals("Debit", transaction.description());
        assertEquals("70.00", service.getBalance(id).balance());
        verify(externalLoggingClient).logBeforeDebit(id, new BigDecimal("30.00"));
    }

    @Test
    void debitWithInsufficientFundsThrowsAndLeavesBalanceUnchanged() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        service.deposit(id, new BigDecimal("20.00"));
        assertThrows(InsufficientFundsException.class, () -> service.debit(id, new BigDecimal("50.00")));
        assertEquals("20.00", service.getBalance(id).balance());
    }

    @Test
    void debitRejectsNonPositiveAmountsBeforeExternalLogging() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        assertThrows(InvalidAmountException.class, () -> service.debit(id, BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> service.debit(id, new BigDecimal("-1.00")));
    }

    @Test
    void debitForMissingAccountThrowsAfterExternalLogging() {
        assertThrows(AccountNotFoundException.class,
                () -> service.debit(999_999L, new BigDecimal("10.00")));
    }

    @Test
    void historyIsNewestFirstAndPagesViaCursor() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);
        for (int i = 1; i <= 5; i++) {
            service.deposit(id, new BigDecimal(i + ".00"));
        }

        TransactionPage page1 = service.history(id, null, 2);
        assertEquals(2, page1.items().size());
        assertNotNull(page1.nextCursor());
        assertTrue(page1.items().get(0).id() > page1.items().get(1).id());

        TransactionPage page2 = service.history(id, page1.nextCursor(), 2);
        assertEquals(2, page2.items().size());
        assertTrue(page2.items().get(0).id() < page1.items().get(1).id());
    }

    @Test
    void historyReturnsEmptySliceForAccountWithoutTransactions() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        TransactionPage page = service.history(id, null, null);

        assertTrue(page.items().isEmpty());
        assertEquals(null, page.nextCursor());
    }

    @Test
    void historyForMissingAccountThrows() {
        assertThrows(AccountNotFoundException.class, () -> service.history(999_999L, null, 10));
    }

    @Test
    void getTransactionReturnsTransactionForAccount() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        TransactionView created = service.deposit(id, new BigDecimal("100.00"));
        TransactionView found = service.getTransaction(id, created.id());

        assertEquals(created.id(), found.id());
        assertEquals(TransactionType.DEPOSIT, found.type());
        assertEquals("100.00", found.amount());
        assertEquals("100.00", found.balanceAfter());
        assertEquals("Deposit", found.description());
    }

    @Test
    void getTransactionForMissingEntryThrows() {
        Long id = service.openAccount(1L, CurrencyCode.EUR);

        assertThrows(TransactionNotFoundException.class, () -> service.getTransaction(id, 999_999L));
    }

    @Test
    void getTransactionForMissingAccountThrows() {
        assertThrows(AccountNotFoundException.class, () -> service.getTransaction(999_999L, 1L));
    }
}
