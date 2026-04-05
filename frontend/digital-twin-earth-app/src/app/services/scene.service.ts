import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SceneQuery, SceneResult } from '../models/scene.model';

@Injectable({ providedIn: 'root' })
export class SceneService {

  private readonly apiBase = '/api';

  constructor(private http: HttpClient) {}

  /**
   * Fetch a combined scene snapshot: all flights + vessels matching the query.
   * The backend returns a SceneResult with both arrays.
   */
  getScene(query: SceneQuery): Observable<SceneResult> {
    let params = new HttpParams();

    if (query.minLat !== undefined) params = params.set('minLat', query.minLat.toString());
    if (query.maxLat !== undefined) params = params.set('maxLat', query.maxLat.toString());
    if (query.minLon !== undefined) params = params.set('minLon', query.minLon.toString());
    if (query.maxLon !== undefined) params = params.set('maxLon', query.maxLon.toString());
    if (query.from)                 params = params.set('from', query.from);
    if (query.to)                   params = params.set('to', query.to);
    if (query.limit !== undefined)  params = params.set('limit', query.limit.toString());
    if (query.entityType)           params = params.set('entityType', query.entityType);

    return this.http.get<SceneResult>(`${this.apiBase}/scene`, { params });
  }

  /**
   * Get a world-level summary: total counts, active regions, etc.
   */
  getSummary(): Observable<{ flightCount: number; vesselCount: number; timestamp: string }> {
    return this.http.get<{ flightCount: number; vesselCount: number; timestamp: string }>(
      `${this.apiBase}/scene/summary`
    );
  }
}
