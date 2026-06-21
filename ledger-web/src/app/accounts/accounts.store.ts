import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { addEntity, setAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, EMPTY, pipe, switchMap, tap } from 'rxjs';
import { AccountSummary, CurrencyCode } from '../core/models/ledger';
import { LedgerApi } from '../core/api/ledger-api';

type AccountsState = { loading: boolean; creating: boolean; error: string | null };
const initialState: AccountsState = { loading: false, creating: false, error: null };

export const AccountsStore = signalStore(
  { providedIn: 'root' },
  withEntities<AccountSummary>(),
  withState(initialState),
  withComputed((store) => ({
    isEmpty: computed(() => !store.loading() && store.entities().length === 0),
  })),
  withMethods((store, api = inject(LedgerApi)) => ({
    load: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          api.listAccounts().pipe(
            tap((accounts) => patchState(store, setAllEntities(accounts), { loading: false })),
            catchError(() => {
              patchState(store, { loading: false, error: 'Failed to load accounts' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
    openAccount: rxMethod<CurrencyCode>(
      pipe(
        tap(() => patchState(store, { creating: true, error: null })),
        switchMap((currency) =>
          api.openAccount(currency).pipe(
            tap((account) => patchState(store, addEntity(account), { creating: false })),
            catchError(() => {
              patchState(store, { creating: false, error: 'Failed to open account' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
  })),
  withHooks({
    onInit(store) { store.load(); },
  }),
);
