import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountOverviewPage } from './account-overview-page';

describe('AccountOverviewPage', () => {
  let component: AccountOverviewPage;
  let fixture: ComponentFixture<AccountOverviewPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountOverviewPage],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountOverviewPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
