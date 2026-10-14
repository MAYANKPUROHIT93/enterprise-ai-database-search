import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { SearchRequest, SearchResponse, HealthStatus, DatabaseSchema } from '../models/mcp.models';

@Injectable({
  providedIn: 'root'
})
export class McpSearchService {

  private readonly baseUrl = 'http://localhost:8090/api';

  constructor(private http: HttpClient) {}

  search(query: string): Observable<SearchResponse> {
    const payload: SearchRequest = { query };
    return this.http.post<SearchResponse>(`${this.baseUrl}/search`, payload).pipe(
      catchError(err => {
        console.error('Search request failed:', err);
        return of({
          query,
          success: false,
          error: err.error?.error || err.message || 'Failed to reach Spring AI orchestrator on port 8090.'
        } as SearchResponse);
      })
    );
  }

  getHealth(): Observable<HealthStatus | null> {
    return this.http.get<HealthStatus>(`${this.baseUrl}/health`).pipe(
      catchError(err => {
        console.error('Health check failed:', err);
        return of(null);
      })
    );
  }

  getSchema(): Observable<DatabaseSchema | null> {
    return this.http.get<DatabaseSchema>(`${this.baseUrl}/schema`).pipe(
      catchError(err => {
        console.error('Schema fetch failed:', err);
        return of(null);
      })
    );
  }
}
