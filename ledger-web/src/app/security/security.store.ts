import { inject } from '@angular/core';
import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { catchError, EMPTY, forkJoin, pipe, switchMap, tap } from 'rxjs';
import { SecurityApi } from '../core/api/security-api';
import {
  Finding,
  FindingStatus,
  PrioritizedVulnerability,
  SecuritySummary,
} from '../core/models/security';

type SecurityState = {
  findings: Finding[];
  summary: SecuritySummary | null;
  vulnerabilities: PrioritizedVulnerability[];
  loading: boolean;
  error: string | null;
};

const initialState: SecurityState = {
  findings: [],
  summary: null,
  vulnerabilities: [],
  loading: false,
  error: null,
};

export const SecurityStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store, api = inject(SecurityApi)) => ({
    load: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          forkJoin({
            findings: api.listFindings(),
            summary: api.summary(),
            vulnerabilities: api.vulnerabilities(),
          }).pipe(
            tap((data) => patchState(store, { ...data, loading: false })),
            catchError(() => {
              patchState(store, { loading: false, error: 'Failed to load security data' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
    setStatus: rxMethod<{ id: number; status: FindingStatus }>(
      pipe(
        switchMap(({ id, status }) =>
          api.updateStatus(id, status).pipe(
            tap((updated) =>
              patchState(store, (state) => ({
                findings: state.findings.map((f) => (f.id === updated.id ? updated : f)),
              })),
            ),
            catchError(() => {
              patchState(store, { error: 'Failed to update the finding' });
              return EMPTY;
            }),
          ),
        ),
      ),
    ),
  })),
  withHooks({
    onInit(store) {
      store.load();
    },
  }),
);
