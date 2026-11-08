import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SecurityStore } from '../security.store';

@Component({
  selector: 'app-security-page',
  imports: [RouterLink, DatePipe],
  templateUrl: './security-page.html',
  styleUrl: './security-page.css',
})
export class SecurityPage {
  protected readonly store = inject(SecurityStore);
  protected readonly hideResolved = signal(true);

  protected readonly visibleFindings = computed(() =>
    this.hideResolved()
      ? this.store.findings().filter((f) => f.status !== 'RESOLVED')
      : this.store.findings(),
  );

  protected toggleResolved(): void {
    this.hideResolved.update((value) => !value);
  }

  protected refresh(): void {
    this.store.load();
  }
}
