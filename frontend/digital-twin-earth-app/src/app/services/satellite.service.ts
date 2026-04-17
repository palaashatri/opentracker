import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SatellitePositionDto } from '../models/satellite.model';
import { SseService } from './sse.service';

@Injectable({ providedIn: 'root' })
export class SatelliteService {
  private readonly apiBase = '/api';

  constructor(private http: HttpClient, private sse: SseService) {}

  getSatellites(): Observable<SatellitePositionDto[]> {
    return this.http.get<SatellitePositionDto[]>(`${this.apiBase}/satellites`);
  }

  streamSatellites(): Observable<SatellitePositionDto> {
    return this.sse.watchEvent<SatellitePositionDto>(`${this.apiBase}/stream/satellites`, 'satellite');
  }
}
