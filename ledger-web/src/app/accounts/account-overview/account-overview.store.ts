import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, EMPTY, filter, forkJoin, pipe, switchMap, tap } from 'rxjs';
import { AccountSummary, LedgerEntry } from '../../core/models/ledger';
import { LedgerApi } from '../../core/api/ledger-api';
import {BalancePoint} from './balance-chart/balance-chart';

const PAGE = 20;

type State = {
  accountId: number | null;
  account: AccountSummary | null;
  entries: LedgerEntry[];
  nextCursor: number | null;
  loading: boolean;       // первичная загрузка (аккаунт + первая страница)
  loadingMore: boolean;   // дозагрузка следующей страницы
  error: string | null;
};

const initial: State = {
  accountId: null, account: null, entries: [],
  nextCursor: null, loading: false, loadingMore: false, error: null,
};

export const AccountOverviewStore = signalStore(
  withState(initial),
  withComputed((store) => ({
    hasMore: computed(() => store.nextCursor() !== null),
    isEmpty: computed(() => !store.loading() && store.entries().length === 0),
    // newest-first → график хочет oldest-first. Деньги остаются строкой;
    // Number() делает сам BalanceChart на отрисовке (единственная граница).
    balanceSeries: computed<BalancePoint[]>(() =>
      [...store.entries()].reverse().map((e) => ({ t: e.createdAt, balance: e.balanceAfter })),
    ),
  })),
  withMethods((store, api = inject(LedgerApi)) => ({
    // Принимает СИГНАЛ accountId из инпута страницы => сам реагирует на смену роута.
    loadAccount: rxMethod<number>(
      pipe(
        tap((id) => patchState(store, {
          ...initial, accountId: id, loading: true,
        })),
        switchMap((id) =>
          forkJoin({
            account: api.getAccount(id),
            page: api.getHistory(id, null, PAGE),
          }).pipe(
            tap(({ account, page }) => patchState(store, {
              account, entries: page.items, nextCursor: page.nextCursor, loading: false,
            })),
            catchError(() => {
              patchState(store, { loading: false, error: 'Failed to load account' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
    loadMore: rxMethod<void>(
      pipe(
        filter(() => store.hasMore() && !store.loadingMore() && !store.loading()),
        tap(() => patchState(store, { loadingMore: true })),
        switchMap(() =>
          api.getHistory(store.accountId()!, store.nextCursor(), PAGE).pipe(
            tap((page) => patchState(store, {
              entries: [...store.entries(), ...page.items],
              nextCursor: page.nextCursor,
              loadingMore: false,
            })),
            catchError(() => {
              patchState(store, { loadingMore: false, error: 'Failed to load more' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
    deposit: rxMethod<string>(
      pipe(
        filter((amount) => store.accountId() != null && amount.trim() !== ''),
        switchMap((amount) =>
          api.deposit(store.accountId()!, amount).pipe(
            switchMap(() =>
              forkJoin({
                account: api.getAccount(store.accountId()!),
                page: api.getHistory(store.accountId()!, null, PAGE),
              }).pipe(
                tap(({ account, page }) =>
                  patchState(store, {
                    account,
                    entries: page.items,
                    nextCursor: page.nextCursor,
                  }),
                ),
              ),
            ),
            catchError(() => {
              patchState(store, { error: 'Deposit failed' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
  })),
);
