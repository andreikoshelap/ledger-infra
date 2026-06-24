import { CurrencyCode, CURRENCY_FRACTION_DIGITS } from './models/ledger';

export function formatMoney(amount: string, currency: CurrencyCode, locale = 'et-EE'): string {
  const digits = CURRENCY_FRACTION_DIGITS[currency];
  return new Intl.NumberFormat(locale, {
    style: 'currency', currency,
    minimumFractionDigits: digits, maximumFractionDigits: digits,
  }).format(Number(amount));
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return [
    `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()}`,
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`,
  ].join(' ');
}

export function normalizeAmount(input: string): string {
  return input.trim().replace(',', '.');
}
