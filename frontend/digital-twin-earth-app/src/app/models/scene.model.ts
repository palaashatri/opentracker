// ============================================================
// Scene / Query Models
// ============================================================

import { AircraftPositionDto, TrackDto } from './aircraft.model';
import { VesselPositionDto } from './vessel.model';

export type EntityType = 'AIRCRAFT' | 'VESSEL';

export type LayerVisibility = {
  flights: boolean;
  ships: boolean;
};

export interface SceneQuery {
  entityType?: EntityType;
  minLat?: number;
  maxLat?: number;
  minLon?: number;
  maxLon?: number;
  from?: string;   // ISO-8601
  to?: string;     // ISO-8601
  limit?: number;
}

export interface SceneResult {
  flights: AircraftPositionDto[];
  vessels: VesselPositionDto[];
  timestamp: string;
}

export interface SelectedEntity {
  type: EntityType;
  id: string;
  data: AircraftPositionDto | VesselPositionDto;
  track?: TrackDto;
}

export interface GlobeStats {
  flightCount: number;
  shipCount: number;
  lastUpdated: Date | null;
  isLive: boolean;
}

export type ConnectionStatus = 'connecting' | 'connected' | 'error' | 'disconnected';

export type PlaybackSpeed = 1 | 2 | 5 | 10;
