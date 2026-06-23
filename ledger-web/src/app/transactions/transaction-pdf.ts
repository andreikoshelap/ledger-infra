import { jsPDF } from 'jspdf';
import { LedgerEntry } from '../core/models/ledger';
import { formatDateTime } from '../core/format';

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
  row('Amount', formatPdfMoney(e.amount, e.currency));
  row('Balance after', formatPdfMoney(e.balanceAfter, e.currency));
  row('Currency', e.currency);
  row('Account', `#${e.accountId}`);
  if (e.counterpartyAccountId) row('Counterparty', `#${e.counterpartyAccountId}`);
  if (e.description) row('Description', e.description);
  row('Date', formatDateTime(e.createdAt));

  doc.save(`transaction-${e.id}.pdf`);
}

function formatPdfMoney(amount: string, currency: string): string {
  return `${amount} ${currency}`;
}
