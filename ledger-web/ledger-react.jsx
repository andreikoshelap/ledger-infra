import React, { useReducer, useMemo, useState, useCallback } from "react";
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts";
import { Plus, Trash2, TrendingUp, TrendingDown, ChevronLeft, ChevronRight, Wallet, RefreshCw, AlertTriangle } from "lucide-react";

/**
 * ---------------------------------------------------------------------------
 * "Redux pattern" without the redux package (not available in this sandbox).
 * The shape is 1:1 with Redux Toolkit's createSlice + dispatch model:
 *   - actions are plain { type, payload } objects
 *   - the reducer is a pure function (state, action) => newState
 *   - dispatch is the only way to mutate state
 * In a real repo this file becomes:
 *   ledgerSlice.ts   -> createSlice({ name: 'ledger', reducers: {...} })
 *   store.ts         -> configureStore({ reducer: { ledger: ledgerReducer } })
 *   LedgerPage.tsx   -> useSelector / useDispatch instead of useReducer
 * The mental model (actions -> reducer -> new state -> re-render) is
 * identical; only the wiring changes. Types below as comments, ready to
 * become real TS interfaces when you rename this to .tsx.
 * ---------------------------------------------------------------------------
 *
 * type Transaction = {
 *   id: string;
 *   date: string;       // ISO yyyy-mm-dd
 *   description: string;
 *   amount: number;      // always positive
 *   type: "credit" | "debit";
 * };
 * type LedgerState = {
 *   transactions: Transaction[];
 *   filter: "all" | "credit" | "debit";
 *   page: number;
 *   status: "idle" | "loading" | "succeeded" | "failed";
 *   error: string | null;
 * };
 *
 * ---------------------------------------------------------------------------
 * Async thunk pattern (mirrors RTK's createAsyncThunk lifecycle).
 * A real createAsyncThunk auto-dispatches three actions for one async call:
 *   fetchTransactions.pending   -> { status: "loading" }
 *   fetchTransactions.fulfilled -> { status: "succeeded", transactions }
 *   fetchTransactions.rejected  -> { status: "failed", error }
 * Below, `fetchTransactions(dispatch)` does the same three dispatches by
 * hand. In a real RTK slice this becomes:
 *   export const fetchTransactions = createAsyncThunk(
 *     "ledger/fetch", async () => api.get("/transactions")
 *   );
 *   extraReducers: (builder) => builder
 *     .addCase(fetchTransactions.pending, (s) => { s.status = "loading"; })
 *     .addCase(fetchTransactions.fulfilled, (s, a) => { s.status = "succeeded"; s.transactions = a.payload; })
 *     .addCase(fetchTransactions.rejected, (s, a) => { s.status = "failed"; s.error = a.error.message; });
 * ---------------------------------------------------------------------------
 */

// Simulated backend call: ~1s latency, ~25% chance of failure (to exercise
// the error path). Swap this for a real fetch("/api/transactions") later.
function mockApiFetchTransactions() {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (Math.random() < 0.25) {
        reject(new Error("Network error: could not reach ledger service"));
      } else {
        const shuffled = [...seedTransactions].map((t) => ({ ...t }));
        resolve(shuffled);
      }
    }, 900);
  });
}

// The "thunk": takes dispatch, orchestrates the pending/fulfilled/rejected
// lifecycle by hand. This is exactly what createAsyncThunk generates for you.
async function fetchTransactions(dispatch) {
  dispatch({ type: "transactions/fetchPending" });
  try {
    const data = await mockApiFetchTransactions();
    dispatch({ type: "transactions/fetchFulfilled", payload: data });
  } catch (err) {
    dispatch({ type: "transactions/fetchRejected", payload: err.message });
  }
}

const PAGE_SIZE = 5;

const seedTransactions = [
  { id: "t1", date: "2026-08-01", description: "Opening balance", amount: 1200, type: "credit" },
  { id: "t2", date: "2026-08-04", description: "Client invoice #1042", amount: 850, type: "credit" },
  { id: "t3", date: "2026-08-06", description: "AWS bill", amount: 96, type: "debit" },
  { id: "t4", date: "2026-08-09", description: "Office supplies", amount: 42, type: "debit" },
  { id: "t5", date: "2026-08-13", description: "Client invoice #1043", amount: 640, type: "credit" },
  { id: "t6", date: "2026-08-15", description: "Contractor payment", amount: 300, type: "debit" },
  { id: "t7", date: "2026-08-18", description: "Domain renewal", amount: 18, type: "debit" },
  { id: "t8", date: "2026-08-21", description: "Client invoice #1044", amount: 410, type: "credit" },
  { id: "t9", date: "2026-08-24", description: "Hosting (Hetzner)", amount: 12, type: "debit" },
];

const initialState = {
  transactions: seedTransactions,
  filter: "all",
  page: 0,
  status: "idle",
  error: null,
};

function ledgerReducer(state, action) {
  switch (action.type) {
    case "transactions/add":
      return { ...state, transactions: [...state.transactions, action.payload], page: 0 };
    case "transactions/remove":
      return { ...state, transactions: state.transactions.filter((t) => t.id !== action.payload) };
    case "filter/set":
      return { ...state, filter: action.payload, page: 0 };
    case "page/set":
      return { ...state, page: action.payload };
    case "transactions/fetchPending":
      return { ...state, status: "loading", error: null };
    case "transactions/fetchFulfilled":
      return { ...state, status: "succeeded", transactions: action.payload, page: 0 };
    case "transactions/fetchRejected":
      return { ...state, status: "failed", error: action.payload };
    default:
      return state;
  }
}

function formatMoney(n) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "EUR" }).format(n);
}

export default function LedgerApp() {
  const [state, dispatch] = useReducer(ledgerReducer, initialState);
  const [form, setForm] = useState({ description: "", amount: "", type: "debit" });
  const [error, setError] = useState("");

  const reload = useCallback(() => {
    fetchTransactions(dispatch);
  }, []);

  // ---- selectors (would be createSelector in RTK) --------------------
  const sortedAsc = useMemo(
    () => [...state.transactions].sort((a, b) => a.date.localeCompare(b.date)),
    [state.transactions]
  );

  const balanceSeries = useMemo(() => {
    let running = 0;
    return sortedAsc.map((t) => {
      running += t.type === "credit" ? t.amount : -t.amount;
      return { date: t.date.slice(5), balance: running };
    });
  }, [sortedAsc]);

  const currentBalance = balanceSeries.length ? balanceSeries[balanceSeries.length - 1].balance : 0;

  const totals = useMemo(() => {
    const credit = state.transactions.filter((t) => t.type === "credit").reduce((s, t) => s + t.amount, 0);
    const debit = state.transactions.filter((t) => t.type === "debit").reduce((s, t) => s + t.amount, 0);
    return { credit, debit };
  }, [state.transactions]);

  const filtered = useMemo(() => {
    const list = [...state.transactions].sort((a, b) => b.date.localeCompare(a.date));
    if (state.filter === "all") return list;
    return list.filter((t) => t.type === state.filter);
  }, [state.transactions, state.filter]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const page = Math.min(state.page, pageCount - 1);
  const pageItems = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE);

  // ---- handlers --------------------------------------------------------
  function handleAdd(e) {
    e.preventDefault();
    const amountNum = parseFloat(form.amount);
    if (!form.description.trim()) {
      setError("Enter a description");
      return;
    }
    if (!amountNum || amountNum <= 0) {
      setError("Enter an amount greater than 0");
      return;
    }
    dispatch({
      type: "transactions/add",
      payload: {
        id: `t${Date.now()}`,
        date: new Date().toISOString().slice(0, 10),
        description: form.description.trim(),
        amount: Math.round(amountNum * 100) / 100,
        type: form.type,
      },
    });
    setForm({ description: "", amount: "", type: "debit" });
    setError("");
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6 font-sans">
      <div className="max-w-3xl mx-auto space-y-6">

        <header className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Wallet className="w-6 h-6 text-teal-400" />
            <h1 className="text-xl font-semibold">Ledger</h1>
          </div>
          <button
            onClick={reload}
            disabled={state.status === "loading"}
            className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border border-slate-700 text-slate-300 hover:text-slate-100 hover:border-slate-500 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${state.status === "loading" ? "animate-spin" : ""}`} />
            {state.status === "loading" ? "Loading…" : "Reload from server"}
          </button>
        </header>

        {state.status === "failed" && (
          <div className="flex items-center justify-between bg-rose-950/40 border border-rose-800 rounded-xl px-4 py-3 text-sm">
            <span className="flex items-center gap-2 text-rose-300">
              <AlertTriangle className="w-4 h-4" /> {state.error}
            </span>
            <button onClick={reload} className="text-rose-300 underline underline-offset-2 hover:text-rose-100">
              Retry
            </button>
          </div>
        )}

        {/* Balance summary */}
        <div className="grid grid-cols-3 gap-3">
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-4">
            <p className="text-xs text-slate-400 mb-1">Current balance</p>
            <p className="text-2xl font-mono font-semibold">{formatMoney(currentBalance)}</p>
          </div>
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-4">
            <p className="text-xs text-slate-400 mb-1 flex items-center gap-1">
              <TrendingUp className="w-3.5 h-3.5 text-emerald-400" /> Credits
            </p>
            <p className="text-lg font-mono text-emerald-400">{formatMoney(totals.credit)}</p>
          </div>
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-4">
            <p className="text-xs text-slate-400 mb-1 flex items-center gap-1">
              <TrendingDown className="w-3.5 h-3.5 text-rose-400" /> Debits
            </p>
            <p className="text-lg font-mono text-rose-400">{formatMoney(totals.debit)}</p>
          </div>
        </div>

        {/* Balance chart */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-4 h-56">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={balanceSeries} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
              <defs>
                <linearGradient id="bal" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#2dd4bf" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#2dd4bf" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="date" stroke="#64748b" tick={{ fontSize: 11 }} />
              <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
              <Tooltip
                contentStyle={{ background: "#1e293b", border: "1px solid #334155", fontSize: 12 }}
                formatter={(v) => formatMoney(v)}
              />
              <Area type="monotone" dataKey="balance" stroke="#2dd4bf" fill="url(#bal)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Add transaction */}
        <form onSubmit={handleAdd} className="bg-slate-800 border border-slate-700 rounded-xl p-4 space-y-3">
          <div className="flex flex-wrap gap-3">
            <input
              className="flex-1 min-w-[160px] bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm"
              placeholder="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
            <input
              className="w-28 bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm font-mono"
              placeholder="0.00"
              value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
            />
            <select
              className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm"
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value })}
            >
              <option value="debit">Debit</option>
              <option value="credit">Credit</option>
            </select>
            <button
              type="submit"
              className="bg-teal-500 hover:bg-teal-400 text-slate-900 font-medium rounded-lg px-4 py-2 text-sm flex items-center gap-1"
            >
              <Plus className="w-4 h-4" /> Add
            </button>
          </div>
          {error && <p className="text-xs text-rose-400">{error}</p>}
        </form>

        {/* Filter + list */}
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex gap-1">
              {["all", "credit", "debit"].map((f) => (
                <button
                  key={f}
                  onClick={() => dispatch({ type: "filter/set", payload: f })}
                  className={`text-xs px-3 py-1.5 rounded-full border ${
                    state.filter === f
                      ? "bg-teal-500 text-slate-900 border-teal-500"
                      : "border-slate-700 text-slate-400 hover:text-slate-200"
                  }`}
                >
                  {f}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <button
                onClick={() => dispatch({ type: "page/set", payload: Math.max(0, page - 1) })}
                disabled={page === 0}
                className="disabled:opacity-30"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              page {page + 1} / {pageCount}
              <button
                onClick={() => dispatch({ type: "page/set", payload: Math.min(pageCount - 1, page + 1) })}
                disabled={page >= pageCount - 1}
                className="disabled:opacity-30"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div className="divide-y divide-slate-700">
            {pageItems.map((t) => (
              <div key={t.id} className="flex items-center justify-between py-2.5 text-sm">
                <div className="flex items-center gap-2 min-w-0">
                  {t.type === "credit" ? (
                    <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0" />
                  ) : (
                    <TrendingDown className="w-4 h-4 text-rose-400 shrink-0" />
                  )}
                  <div className="min-w-0">
                    <p className="truncate">{t.description}</p>
                    <p className="text-xs text-slate-500">{t.date}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <span className={`font-mono ${t.type === "credit" ? "text-emerald-400" : "text-rose-400"}`}>
                    {t.type === "credit" ? "+" : "-"}
                    {formatMoney(t.amount)}
                  </span>
                  <button
                    onClick={() => dispatch({ type: "transactions/remove", payload: t.id })}
                    className="text-slate-500 hover:text-rose-400"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
            {pageItems.length === 0 && (
              <p className="text-sm text-slate-500 py-6 text-center">No transactions on this page.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
