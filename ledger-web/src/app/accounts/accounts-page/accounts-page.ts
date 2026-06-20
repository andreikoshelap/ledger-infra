import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountsStore } from '../accounts.store';
import { formatMoney } from '../../core/money/format';

@Component({
  selector: 'app-accounts-page',
  imports: [RouterLink],
  templateUrl: './accounts-page.html',
  styleUrl: './accounts-page.css',
})
export class AccountsPage {
  protected readonly store = inject(AccountsStore);
  protected readonly formatMoney = formatMoney;
}
