import {Component, computed, effect, inject, input, numberAttribute, signal} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountOverviewStore } from '../account-overview.store';
import { TransactionList } from '../transaction-list/transaction-list';
import { formatMoney } from '../../../core/format';
import { BalanceChart } from '../balance-chart/balance-chart';
import { AccountsStore } from '../../accounts.store';
import {LedgerApi} from '../../../core/api/ledger-api';
import {toObservable, toSignal} from '@angular/core/rxjs-interop';
import {catchError, combineLatest, debounceTime, of, switchMap} from 'rxjs';

@Component({
  selector: 'app-account-overview-page',
  imports: [RouterLink, TransactionList, BalanceChart],
  providers: [AccountOverviewStore],           // per-route: fresh store on any account
  templateUrl: './account-overview-page.html',
  styleUrls: ['./account-overview-page.css'],
})
export class AccountOverviewPage {
  private readonly api = inject(LedgerApi);
  readonly accountId = input.required({ transform: numberAttribute }); // ':accountId' from the route
  protected readonly store = inject(AccountOverviewStore);
  protected readonly formatMoney = formatMoney;
  protected readonly accounts = inject(AccountsStore);

  protected readonly others = computed(() =>
    this.accounts.entities().filter((a) => a.id !== this.accountId()),
  );
  protected readonly selectedTo = signal<number | null>(null);
  protected readonly amount = signal('');
  protected readonly hasExchangeTarget = computed(() => this.selectedTo() != null);


  constructor() {
    this.store.loadAccount(this.accountId);     // pass the signal so it reacts to account changes
    effect(() => {
      const options = this.others();
      const current = this.selectedTo();

      if (options.length === 1) {
        const onlyId = options[0].id;
        if (current !== onlyId) this.selectedTo.set(onlyId);
        return;
      }

      if (current != null && !options.some((account) => account.id === current)) {
        this.selectedTo.set(null);
      }
    });
  }

  protected onExchangeTargetChange(rawValue: string): void {
    if (rawValue === '') {
      this.selectedTo.set(null);
      return;
    }
    this.selectedTo.set(Number(rawValue));
  }

  protected onExchangeAmountInput(rawValue: string): void {
    this.amount.set(rawValue);
  }

  protected readonly quote = toSignal(
    combineLatest([toObservable(this.selectedTo), toObservable(this.amount)]).pipe(
      debounceTime(300),
      switchMap(([to, amt]) =>
        to != null && amt.trim() !== ''
          ? this.api.quote(this.accountId(), to, amt).pipe(catchError(() => of(null)))
          : of(null),
      ),
    ),
  );

  protected doExchange(amount: string): void {
    const to = this.selectedTo();
    if (to != null) {
      this.store.exchange({ toAccountId: to, amount });
      this.amount.set('');
    }
  }
}
