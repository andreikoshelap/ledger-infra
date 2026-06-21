import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'accounts',
  },
  {
    path: 'accounts',
    title: 'Accounts',
    loadComponent: () =>
      import('./accounts/accounts-page/accounts-page').then(m => m.AccountsPage),
  },
  {
    path: 'accounts/:accountId/tx/:entryId',
    title: 'Transaction',
    loadComponent: () =>
      import('./transactions/transaction-overview-page/transaction-overview-page')
        .then(m => m.TransactionOverviewPage),
  },
  {
    path: 'accounts/:accountId',
    title: 'Account',
    loadComponent: () =>
      import('./accounts/account-overview/account-overview-page/account-overview-page')
        .then(m => m.AccountOverviewPage),
  },
  { path: '**', redirectTo: 'accounts' },
];
