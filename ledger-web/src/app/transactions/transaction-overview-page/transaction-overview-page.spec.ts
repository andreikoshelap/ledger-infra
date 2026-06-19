import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionOverviewPage } from './transaction-overview-page';

describe('TransactionOverviewPage', () => {
  let component: TransactionOverviewPage;
  let fixture: ComponentFixture<TransactionOverviewPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionOverviewPage],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionOverviewPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
