import { TestBed } from '@angular/core/testing';

import { LedgerApi } from './ledger-api';

describe('LedgerApi', () => {
  let service: LedgerApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LedgerApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
