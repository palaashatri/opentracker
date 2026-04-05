// ============================================================
// Aircraft / Flight Models
// ============================================================

export interface Aircraft {
  id: string;
  icao24: string;
  callsign: string | null;
  airline: string | null;
  model: string | null;
  country: string | null;
  source: string;
}

export interface AircraftPositionDto {
  id: string;
  aircraftId: string;
  icao24: string;
  callsign: string | null;
  timestamp: string;          // ISO-8601
  lat: number;
  lon: number;
  altitudeMeters: number | null;
  groundSpeedMps: number | null;
  headingDeg: number | null;
  verticalRateMps: number | null;
}

export interface TrackPoint {
  timestamp: string;
  lat: number;
  lon: number;
  altitudeMeters: number | null;
  headingDeg: number | null;
}

export interface TrackDto {
  entityId: string;
  entityType: string;         // 'AIRCRAFT' | 'VESSEL'
  points: TrackPoint[];
}

export interface BoundingBox {
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}
