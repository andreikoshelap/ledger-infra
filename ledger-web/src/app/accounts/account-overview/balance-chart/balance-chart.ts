import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  CategoryScale,
  Chart,
  ChartConfiguration,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { CurrencyCode } from '../../../core/models/ledger';

Chart.register(LineController, LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Filler);

export interface BalancePoint {
  t: string;       // createdAt, ISO
  balance: string; // signed money string: "1234.00"
}

@Component({
  selector: 'app-balance-chart',
  template: `<canvas #canvas></canvas>`,
// A fixed height is required: responsive Chart.js with maintainAspectRatio:false stretches to the container
  styles: `:host { display: block; position: relative; height: 220px; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalanceChart {
  readonly series = input.required<BalancePoint[]>();
  readonly currency = input.required<CurrencyCode>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart<'line'>;

    // Intl already knows the currency scale (VND -> 0 digits), so no manual VND special case is needed.
  private readonly money = computed(
    () => new Intl.NumberFormat(undefined, { style: 'currency', currency: this.currency() }),
  );
  private readonly dateFmt = new Intl.DateTimeFormat(undefined, {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  });

  constructor() {
    const destroyRef = inject(DestroyRef);

    // The canvas exists only after the first render, so create the chart here instead of inside an effect.
    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.config());
      this.sync();
    });

      // Rebuild data on any signal change; this runs outside Angular CD, which is fine for zoneless mode.
    effect(() => {
      this.series();      // tracked
      this.currency();    // tracked
      this.sync();
    });

    destroyRef.onDestroy(() => this.chart?.destroy());
  }

  private sync(): void {
    const chart = this.chart;
    if (!chart) return; // the effect may run before afterNextRender, which is fine

    const pts = this.series();
    chart.data.labels = pts.map((p) => this.dateFmt.format(new Date(p.t)));
    // This is the only string-to-number boundary in the money flow. No arithmetic, rendering only.
    chart.data.datasets[0].data = pts.map((p) => Number(p.balance));
    chart.update('none'); // avoid re-animating the whole chart for every newly loaded slice
  }

  private config(): ChartConfiguration<'line'> {
    return {
      type: 'line',
      data: {
        labels: [],
        datasets: [{ data: [], fill: true, tension: 0.25, pointRadius: 2, borderWidth: 2 }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => this.money().format(Number(ctx.parsed.y)),
            },
          },
        },
        scales: {
          y: { ticks: { callback: (v) => this.money().format(Number(v)) } },
        },
      },
    };
  }
}
