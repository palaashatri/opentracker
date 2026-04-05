import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AircraftPositionDto, BoundingBox, TrackDto } from '../models/aircraft.model';
import { SseService } from './sse.service';

@Injectable({ providedIn: 'root' })
export class FlightService {

  private readonly apiBase = '/api';

  constructor(
    private http: HttpClient,
    private sse: SseService
  ) {}

  /**
   * Fetch a snapshot of all flights within the given bounding box.
   */
  getFlights(bbox: BoundingBox): Observable<AircraftPositionDto[]> {
    const params = new HttpParams()
      .set('minLat', bbox.minLat.toString())
      .set('maxLat', bbox.maxLat.toString())
      .set('minLon', bbox.minLon.toString())
      .set('maxLon', bbox.maxLon.toString());

    return this.http.get<AircraftPositionDto[]>(`${this.apiBase}/flights`, { params });
  }

  /**
   * Fetch a single flight's latest position.
   */
  getFlight(id: string): Observable<AircraftPositionDto> {
    return this.http.get<AircraftPositionDto>(`${this.apiBase}/flights/${id}`);
  }

  /**
   * Fetch the historical track for a flight between two dates.
   */
  getTrack(id: string, from: Date, to: Date): Observable<TrackDto> {
    const params = new HttpParams()
      .set('from', from.toISOString())
      .set('to', to.toISOString());

    return this.http.get<TrackDto>(`${this.apiBase}/flights/${id}/track`, { params });
  }

  /**
   * Stream live flight position updates via SSE.
   */
  streamFlights(): Observable<AircraftPositionDto> {
    return this.sse.watch<AircraftPositionDto>(`${this.apiBase}/stream/flights`);
  }

  /**
   * Stream live updates for a specific flight.
   */
  streamFlight(icao24: string): Observable<AircraftPositionDto> {
    return this.sse.watch<AircraftPositionDto>(`${this.apiBase}/stream/flights/${icao24}`);
  }
}
