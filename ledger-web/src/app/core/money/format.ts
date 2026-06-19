import { CurrencyCode, CURRENCY_FRACTION_DIGITS } from '../models/ledger';

export function formatMoney(amount: string, currency: CurrencyCode, locale = 'et-EE'): string {
  const digits = CURRENCY_FRACTION_DIGITS[currency];
  return new Intl.NumberFormat(locale, {
    style: 'currency', currency,
    minimumFractionDigits: digits, maximumFractionDigits: digits,
  }).format(Number(amount));
}
