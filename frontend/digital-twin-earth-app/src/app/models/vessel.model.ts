// ============================================================
// Vessel / Ship Models
// ============================================================

export type VesselType =
  | 'CARGO'
  | 'TANKER'
  | 'PASSENGER'
  | 'FISHING'
  | 'SAILING'
  | 'PLEASURE_CRAFT'
  | 'HIGH_SPEED'
  | 'MILITARY'
  | 'OTHER';

export interface Vessel {
  id: string;
  mmsi: string;
  imo: string | null;
  name: string | null;
  callsign: string | null;
  flag: string | null;
  vesselType: VesselType | null;
  lengthMeters: number | null;
  widthMeters: number | null;
  source: string;
}

export interface VesselPositionDto {
  id: string;
  vesselId: string;
  mmsi: string;
  name: string | null;
  timestamp: string;          // ISO-8601
  lat: number;
  lon: number;
  speedKnots: number | null;
  courseDeg: number | null;
}

export interface VesselTrackPoint {
  timestamp: string;
  lat: number;
  lon: number;
  speedKnots: number | null;
  courseDeg: number | null;
}
