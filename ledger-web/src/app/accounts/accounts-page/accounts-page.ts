import {Component, inject, signal} from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountsStore } from '../accounts.store';
import { formatMoney } from '../../core/money/format';
import {CurrencyCode} from '../../core/models/ledger';

@Component({
  selector: 'app-accounts-page',
  imports: [RouterLink],
  templateUrl: './accounts-page.html',
  styleUrl: './accounts-page.css',
})
export class AccountsPage {
  protected readonly store = inject(AccountsStore);
  protected readonly formatMoney = formatMoney;
  protected readonly currencies: CurrencyCode[] = ['EUR', 'USD', 'SEK', 'GBP', 'VND'];
  protected readonly selected = signal<CurrencyCode>('EUR');

  protected open(): void {
    this.store.openAccount(this.selected());
  }

}
