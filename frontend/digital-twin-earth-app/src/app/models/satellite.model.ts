export interface SatellitePositionDto {
  id: string;
  noradId: string;
  name: string | null;
  timestamp: string;
  lat: number;
  lon: number;
  altitudeKm: number;
  velocityKmS: number;
}
