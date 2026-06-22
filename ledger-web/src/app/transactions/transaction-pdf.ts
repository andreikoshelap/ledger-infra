import { jsPDF } from 'jspdf';
import { LedgerEntry } from '../core/models/ledger';
import { formatMoney } from '../core/money/format';
// import { formatDateTime } from '../core/money/format'; // или где у тебя хелпер даты

export function downloadTransactionPdf(e: LedgerEntry): void {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' });
  let y = 64;

  doc.setFontSize(18);
  doc.text('Transaction summary', 64, y);
  y += 36;

  doc.setFontSize(11);
  const row = (label: string, value: string) => {
    doc.setTextColor(120);
    doc.text(label, 64, y);
    doc.setTextColor(20);
    doc.text(value, 220, y);
    y += 22;
  };

  row('Transaction', `#${e.id}`);
  row('Type', e.type);
  row('Amount', formatMoney(e.amount, e.currency));         // строка → формат, без арифметики
  row('Balance after', formatMoney(e.balanceAfter, e.currency));
  row('Currency', e.currency);
  row('Account', `#${e.accountId}`);
  if (e.counterpartyAccountId) row('Counterparty', `#${e.counterpartyAccountId}`);
  if (e.description) row('Description', e.description);
  // row('Date', formatDateTime(e.createdAt));

  doc.save(`transaction-${e.id}.pdf`);
}
