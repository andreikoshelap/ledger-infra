import { Component, inject, input, numberAttribute } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';
import { LedgerApi } from '../../core/api/ledger-api';
import { formatMoney } from '../../core/money/format';
import { downloadTransactionPdf } from '../transaction-pdf';

@Component({
  selector: 'app-transaction-overview-page',
  imports: [RouterLink],
  templateUrl: './transaction-overview-page.html',
  styleUrl: './transaction-overview-page.css',
})
export class TransactionOverviewPage {
  readonly accountId = input.required({ transform: numberAttribute });
  readonly entryId = input.required({ transform: numberAttribute });

  private readonly api = inject(LedgerApi);
  protected readonly formatMoney = formatMoney;

  // оба id — сигналы из роута; реагируем на смену, дёргаем getTransaction
  protected readonly entry = toSignal(
    toObservable(this.entryId).pipe(
      switchMap((entryId) => this.api.getTransaction(this.accountId(), entryId)),
    ),
  );

  protected downloadPdf(): void {
    const e = this.entry();
    if (e) downloadTransactionPdf(e);   // guard: entry() может быть undefined до загрузки
  }
}
