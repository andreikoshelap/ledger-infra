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
  balance: string; // знаковая money-строка: "1234.00"
}

@Component({
  selector: 'app-balance-chart',
  template: `<canvas #canvas></canvas>`,
  // фикс. высота обязательна: Chart.js responsive + maintainAspectRatio:false тянется по контейнеру
  styles: `:host { display: block; position: relative; height: 220px; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BalanceChart {
  readonly series = input.required<BalancePoint[]>();
  readonly currency = input.required<CurrencyCode>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart<'line'>;

  // Intl сам знает scale валюты (VND → 0 знаков) — никакого ручного спец-кейса VND.
  private readonly money = computed(
    () => new Intl.NumberFormat(undefined, { style: 'currency', currency: this.currency() }),
  );
  private readonly dateFmt = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' });

  constructor() {
    const destroyRef = inject(DestroyRef);

    // canvas существует только после первого рендера → создаём график здесь, не в effect.
    afterNextRender(() => {
      this.chart = new Chart(this.canvas().nativeElement, this.config());
      this.sync();
    });

    // данные перекладываем при любом изменении сигналов; работает вне Angular CD — для zoneless ок.
    effect(() => {
      this.series();      // tracked
      this.currency();    // tracked
      this.sync();
    });

    destroyRef.onDestroy(() => this.chart?.destroy());
  }

  private sync(): void {
    const chart = this.chart;
    if (!chart) return; // effect может сработать раньше afterNextRender — это нормально

    const pts = this.series();
    chart.data.labels = pts.map((p) => this.dateFmt.format(new Date(p.t)));
    // ЕДИНСТВЕННАЯ граница string→number во всём money-флоу. Никакой арифметики, только отрисовка.
    chart.data.datasets[0].data = pts.map((p) => Number(p.balance));
    chart.update('none'); // без переанимации всего графика на каждый догруженный кусок
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
