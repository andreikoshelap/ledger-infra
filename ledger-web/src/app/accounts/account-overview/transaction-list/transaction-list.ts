import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LedgerEntry } from '../../../core/models/ledger';
import { formatMoney } from '../../../core/money/format';
import { IntersectDirective } from '../../../core/intersect';

@Component({
  selector: 'app-transaction-list',
  imports: [DatePipe, RouterLink, IntersectDirective],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.css',
})
export class TransactionList {
  readonly entries = input.required<LedgerEntry[]>();
  readonly hasMore = input.required<boolean>();
  readonly loadingMore = input.required<boolean>();
  readonly accountId = input.required<number>();
  readonly loadMore = output<void>();
  protected readonly formatMoney = formatMoney;
}
