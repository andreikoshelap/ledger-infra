import { afterNextRender, Directive, ElementRef, inject, OnDestroy, output } from '@angular/core';

@Directive({ selector: '[appIntersect]' })
export class IntersectDirective implements OnDestroy {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly intersect = output<void>();
  private observer?: IntersectionObserver;

  constructor() {
    afterNextRender(() => {
      this.observer = new IntersectionObserver(
        (entries) => entries.some((e) => e.isIntersecting) && this.intersect.emit(),
        { rootMargin: '200px' }, // префетч заранее, до самого низа
      );
      this.observer.observe(this.host.nativeElement);
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
