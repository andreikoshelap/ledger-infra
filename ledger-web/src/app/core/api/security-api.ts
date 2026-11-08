import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Finding,
  FindingStatus,
  PrioritizedVulnerability,
  SecuritySummary,
} from '../models/security';

@Injectable({ providedIn: 'root' })
export class SecurityApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/security';

  listFindings(): Observable<Finding[]> {
    return this.http.get<Finding[]>(`${this.base}/findings`);
  }

  updateStatus(id: number, status: FindingStatus): Observable<Finding> {
    return this.http.post<Finding>(`${this.base}/findings/${id}/status`, { status });
  }

  summary(): Observable<SecuritySummary> {
    return this.http.get<SecuritySummary>(`${this.base}/summary`);
  }

  vulnerabilities(): Observable<PrioritizedVulnerability[]> {
    return this.http.get<PrioritizedVulnerability[]>(`${this.base}/vulnerabilities`);
  }
}
