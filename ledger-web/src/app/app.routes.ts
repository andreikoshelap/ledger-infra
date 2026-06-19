import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Accounts',
    loadComponent: () =>
      import('./accounts/accounts-page/accounts-page').then(m => m.AccountsPage),
  },
  {
    path: 'accounts/:accountId',
    title: 'Account',
    loadComponent: () =>
      import('./accounts/account-overview/account-overview-page/account-overview-page')
        .then(m => m.AccountOverviewPage),
  },
  {
    path: 'accounts/:accountId/tx/:entryId',
    title: 'Transaction',
    loadComponent: () =>
      import('./transactions/transaction-overview-page/transaction-overview-page')
        .then(m => m.TransactionOverviewPage),
  },
  { path: '**', redirectTo: '' },
];
