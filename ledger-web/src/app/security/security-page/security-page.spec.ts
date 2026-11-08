import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Finding, PrioritizedVulnerability, SecuritySummary } from '../../core/models/security';
import { SecurityPage } from './security-page';

const finding: Finding = {
  id: 1,
  ruleId: 'LEDGER-001',
  title: "Access to another user's account (BOLA)",
  severity: 'HIGH',
  riskScore: 90,
  status: 'OPEN',
  owaspCategory: 'A01:2025 Broken Access Control',
  attackTactic: 'Initial Access (TA0001)',
  attackTechnique: 'T1190 Exploit Public-Facing Application',
  actor: '7',
  sourceIp: '203.0.113.66',
  affectedAccountId: 1,
  occurrenceCount: 12,
  firstSeen: '2026-11-08T10:00:00Z',
  lastSeen: '2026-11-08T10:05:00Z',
  evidence: 'user 7 GET /api/accounts/1/balance on account 1 owned by user 1 -> HTTP 200',
  intel: 'MALICIOUS: sample-threat-feed',
};

const summary: SecuritySummary = {
  rawEvents: 120,
  detections: 14,
  suppressedDuplicates: 11,
  findingsCreated: 3,
  activeFindings: 3,
  noiseReductionPct: 97.5,
};

const vulnerability: PrioritizedVulnerability = {
  finding: {
    cveId: 'CVE-2099-0001',
    component: 'sample-http-client',
    version: '1.4.2',
    cvss: 9.8,
    epss: 0.92,
    knownExploited: true,
    loadedAtRuntime: true,
    internetExposed: true,
  },
  priorityScore: 97,
  tier: 'P1 - fix now',
  rationale: 'CVSS 9.8, EPSS 92%, known exploited, internet-exposed, loaded at runtime',
};

describe('SecurityPage', () => {
  let fixture: ComponentFixture<SecurityPage>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecurityPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SecurityPage);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function flushBackend(): void {
    http.expectOne('/api/security/findings').flush([finding]);
    http.expectOne('/api/security/summary').flush(summary);
    http.expectOne('/api/security/vulnerabilities').flush([vulnerability]);
  }

  it('renders findings, noise metrics and prioritised vulnerabilities', async () => {
    flushBackend();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Access to another user');
    expect(text).toContain('×12');
    expect(text).toContain('97.5%');
    expect(text).toContain('CVE-2099-0001');
    expect(text).toContain('P1 - fix now');
  });
});
