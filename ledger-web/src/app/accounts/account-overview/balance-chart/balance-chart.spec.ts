import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BalanceChart } from './balance-chart';

describe('BalanceChart', () => {
  let component: BalanceChart;
  let fixture: ComponentFixture<BalanceChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BalanceChart],
    }).compileComponents();

    fixture = TestBed.createComponent(BalanceChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
