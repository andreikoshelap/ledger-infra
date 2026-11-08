export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type FindingStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

/** A deduplicated, enriched and scored security finding. */
export interface Finding {
  id: number;
  ruleId: string;
  title: string;
  severity: Severity;
  riskScore: number;
  status: FindingStatus;
  owaspCategory: string | null;
  attackTactic: string | null;
  attackTechnique: string | null;
  actor: string | null;
  sourceIp: string | null;
  affectedAccountId: number | null;
  occurrenceCount: number;
  firstSeen: string;
  lastSeen: string;
  evidence: string | null;
  intel: string | null;
}

/** Noise metrics since the backend started. */
export interface SecuritySummary {
  rawEvents: number;
  detections: number;
  suppressedDuplicates: number;
  findingsCreated: number;
  activeFindings: number;
  noiseReductionPct: number;
}

export interface VulnerabilityFinding {
  cveId: string;
  component: string;
  version: string;
  cvss: number;
  epss: number;
  knownExploited: boolean;
  loadedAtRuntime: boolean;
  internetExposed: boolean;
}

export interface PrioritizedVulnerability {
  finding: VulnerabilityFinding;
  priorityScore: number;
  tier: string;
  rationale: string;
}
