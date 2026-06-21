import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AccountSummary, CurrencyCode, LedgerEntry, TransactionPage } from '../models/ledger';

@Injectable({ providedIn: 'root' })
export class LedgerApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';
  listAccounts(): Observable<AccountSummary[]> {
    return this.http.get<AccountSummary[]>(`${this.base}/accounts`);
  }

  openAccount(currency: CurrencyCode): Observable<AccountSummary> {
    return this.http.post<AccountSummary>(`${this.base}/accounts`, { currency });
  }

  getAccount(accountId: number): Observable<AccountSummary> {
    return this.http.get<AccountSummary>(`${this.base}/accounts/${accountId}`);
  }

  getHistory(accountId: number, cursor: number | null, limit = 20): Observable<TransactionPage> {
    let params = new HttpParams().set('limit', limit);
    if (cursor != null) {
      params = params.set('cursor', cursor);
    }
    return this.http.get<TransactionPage>(`${this.base}/accounts/${accountId}/transactions`, { params });
  }

  getTransaction(accountId: number, entryId: number): Observable<LedgerEntry> {
    return this.http.get<LedgerEntry>(`${this.base}/accounts/${accountId}/transactions/${entryId}`);
  }

  deposit(accountId: number, amount: string): Observable<void> {
    return this.http.post<void>(`${this.base}/accounts/${accountId}/deposit`, { amount });
  }
}
