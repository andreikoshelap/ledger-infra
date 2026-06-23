export type CurrencyCode = 'EUR' | 'USD' | 'SEK' | 'GBP' | 'VND';

export const CURRENCY_FRACTION_DIGITS: Record<CurrencyCode, number> = {
  EUR: 2, USD: 2, SEK: 2, GBP: 2, VND: 0,
};

export type TransactionType = 'DEPOSIT' | 'DEBIT' | 'EXCHANGE_OUT' | 'EXCHANGE_IN';

export interface AccountSummary {
  id: number;
  currency: CurrencyCode;
  balance: string;
}

export interface LedgerEntry {
  id: number;
  transactionId: number;
  accountId: number;
  type: TransactionType;
  amount: string;
  balanceAfter: string;
  currency: CurrencyCode;
  counterpartyAccountId: number | null;
  description: string | null;
  createdAt: string;        // ISO instant
}

export interface TransactionPage {
  items: LedgerEntry[];
  nextCursor: number | null;
}

export interface Quote { amount: string; converted: string; from: CurrencyCode; to: CurrencyCode; }

export interface BalancePoint { t: string; balance: string; }
