import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { setAllEntities, withEntities } from '@ngrx/signals/entities';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, EMPTY, pipe, switchMap, tap } from 'rxjs';
import { AccountSummary } from '../core/models/ledger';
import { LedgerApi } from '../core/api/ledger-api';

type AccountsState = { loading: boolean; error: string | null };
const initialState: AccountsState = { loading: false, error: null };

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
  })),
  withHooks({
    onInit(store) { store.load(); },
  }),
);
