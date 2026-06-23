import {Component, computed, effect, inject, input, numberAttribute, signal} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountOverviewStore } from '../account-overview.store';
import { TransactionList } from '../transaction-list/transaction-list';
import { formatMoney } from '../../../core/money/format';
import { BalanceChart } from '../balance-chart/balance-chart';
import { AccountsStore } from '../../accounts.store';

@Component({
  selector: 'app-account-overview-page',
  imports: [RouterLink, TransactionList, BalanceChart],
  providers: [AccountOverviewStore],           // per-route: fresh store on any account
  templateUrl: './account-overview-page.html',
  styleUrls: ['./account-overview-page.css'],
})
export class AccountOverviewPage {
  readonly accountId = input.required({ transform: numberAttribute }); // ':accountId' из роута
  protected readonly store = inject(AccountOverviewStore);
  protected readonly formatMoney = formatMoney;
  protected readonly accounts = inject(AccountsStore);

  constructor() {
    this.store.loadAccount(this.accountId);     // передаём СИГНАЛ -> реагирует на смену счёта
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

  protected readonly others = computed(() =>
    this.accounts.entities().filter((a) => a.id !== this.accountId()),
  );
  protected selectedTo = signal<number | null>(null);
  protected readonly hasExchangeTarget = computed(() => this.selectedTo() != null);

  protected onExchangeTargetChange(rawValue: string): void {
    if (rawValue === '') {
      this.selectedTo.set(null);
      return;
    }

    this.selectedTo.set(Number(rawValue));
  }

  protected doExchange(amount: string): void {
    const to = this.selectedTo();
    if (to != null) this.store.exchange({ toAccountId: to, amount });
  }
}
