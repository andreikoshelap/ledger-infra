import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountOverviewStore } from '../account-overview.store';
import { TransactionList } from '../transaction-list/transaction-list';
import { formatMoney } from '../../../core/money/format';
import { BalanceChart } from '../balance-chart/balance-chart';

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

  constructor() {
    this.store.loadAccount(this.accountId);     // передаём СИГНАЛ -> реагирует на смену счёта
  }
}
