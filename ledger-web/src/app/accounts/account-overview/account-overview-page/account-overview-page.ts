import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountOverviewStore } from '../account-overview.store';
import { TransactionList } from '../transaction-list/transaction-list';
import { formatMoney } from '../../../core/money/format';

@Component({
  selector: 'app-account-overview-page',
  imports: [RouterLink, TransactionList],
  providers: [AccountOverviewStore],           // per-route: свежий стор на каждый счёт
  templateUrl: './account-overview-page.html',
  styleUrl: './account-overview-page.css',
})
export class AccountOverviewPage {
  readonly accountId = input.required({ transform: numberAttribute }); // ':accountId' из роута
  protected readonly store = inject(AccountOverviewStore);
  protected readonly formatMoney = formatMoney;

  constructor() {
    this.store.loadAccount(this.accountId);     // передаём СИГНАЛ -> реагирует на смену счёта
  }
}
