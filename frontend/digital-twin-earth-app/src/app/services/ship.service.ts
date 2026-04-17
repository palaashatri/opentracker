import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoundingBox, TrackDto } from '../models/aircraft.model';
import { VesselPositionDto } from '../models/vessel.model';
import { SseService } from './sse.service';

@Injectable({ providedIn: 'root' })
export class ShipService {

  private readonly apiBase = '/api';

  constructor(
    private http: HttpClient,
    private sse: SseService
  ) {}

  /**
   * Fetch a snapshot of all vessels within the given bounding box.
   */
  getShips(bbox: BoundingBox): Observable<VesselPositionDto[]> {
    const params = new HttpParams()
      .set('minLat', bbox.minLat.toString())
      .set('maxLat', bbox.maxLat.toString())
      .set('minLon', bbox.minLon.toString())
      .set('maxLon', bbox.maxLon.toString());

    return this.http.get<VesselPositionDto[]>(`${this.apiBase}/ships`, { params });
  }

  /**
   * Fetch a single vessel's latest position.
   */
  getShip(mmsi: string): Observable<VesselPositionDto> {
    return this.http.get<VesselPositionDto>(`${this.apiBase}/ships/${mmsi}`);
  }

  /**
   * Fetch the historical track for a vessel between two dates.
   */
  getTrack(mmsi: string, from: Date, to: Date): Observable<TrackDto> {
    const params = new HttpParams()
      .set('from', from.toISOString())
      .set('to', to.toISOString());

    return this.http.get<TrackDto>(`${this.apiBase}/ships/${mmsi}/track`, { params });
  }

  /**
   * Stream live vessel position updates via SSE.
   */
  streamShips(): Observable<VesselPositionDto> {
    return this.sse.watchEvent<VesselPositionDto>(`${this.apiBase}/stream/ships`, 'ship');
  }

  /**
   * Stream live updates for a specific vessel.
   */
  streamShip(mmsi: string): Observable<VesselPositionDto> {
    return this.sse.watch<VesselPositionDto>(`${this.apiBase}/stream/vessels/${mmsi}`);
  }
}
