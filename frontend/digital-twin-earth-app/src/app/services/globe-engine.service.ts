import { Injectable } from '@angular/core';
import { AircraftPositionDto, TrackPoint } from '../models/aircraft.model';
import { VesselPositionDto } from '../models/vessel.model';
import { SatellitePositionDto } from '../models/satellite.model';

// CesiumJS is loaded as a global <script> via angular.json.
// Declaring it as `any` avoids requiring the @types/cesium package
// while still giving us full runtime access.
declare const Cesium: any; // eslint-disable-line @typescript-eslint/no-explicit-any

@Injectable({ providedIn: 'root' })
export class GlobeEngineService {

  private viewer: any = null; // eslint-disable-line @typescript-eslint/no-explicit-any
  private readonly flightEntities    = new Map<string, any>(); // keyed by icao24
  private readonly shipEntities      = new Map<string, any>(); // keyed by mmsi
  private readonly trackEntities     = new Map<string, any>(); // keyed by entityId
  private readonly satelliteEntities = new Map<string, any>(); // keyed by noradId

  // ── Initialisation ─────────────────────────────────────────

  init(container: HTMLElement): void {
    // Tell CesiumJS where to find its static assets (workers, imagery, etc.)
    // Must be set before new Cesium.Viewer() is called.
    (window as any)['CESIUM_BASE_URL'] = '/cesium';

    // Opt out of Cesium Ion entirely — all imagery/terrain comes from free, no-key providers.
    // Without this, Cesium makes Ion token requests that silently fail and can block rendering.
    Cesium.Ion.defaultAccessToken = undefined;

    // In CesiumJS 1.103+, passing `imageryProvider` to the Viewer constructor is
    // deprecated and silently ignored — the globe stays blue. The correct approach is:
    //   1. Pass `baseLayer: false` to suppress the default Ion imagery
    //   2. Add OSM manually via imageryLayers after construction
    this.viewer = new Cesium.Viewer(container, {
      animation:                              false,
      baseLayerPicker:                        false,
      fullscreenButton:                       false,
      geocoder:                               false,
      homeButton:                             false,
      infoBox:                                false,
      sceneModePicker:                        false,
      selectionIndicator:                     false,
      timeline:                               false,
      navigationHelpButton:                   false,
      navigationInstructionsInitiallyVisible: false,
      scene3DOnly:                            true,
      shouldAnimate:                          true,
      baseLayer:                              false,  // suppress default Ion imagery
    });

    // ── Imagery — OpenStreetMap (free, no key, ODbL license) ────────────────
    this.viewer.imageryLayers.addImageryProvider(
      new Cesium.UrlTemplateImageryProvider({
        url:          'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
        credit:       '© OpenStreetMap contributors',
        maximumLevel: 19,
      })
    );

    // ── Terrain — EllipsoidTerrainProvider (no Ion token required) ──────────
    // createWorldTerrainAsync requires a valid Ion token; use the flat ellipsoid
    // instead so terrain always works without credentials.
    this.viewer.terrainProvider = new Cesium.EllipsoidTerrainProvider();

    // ── Scene aesthetics ─────────────────────────────────────────────────────
    const scene = this.viewer.scene;

    scene.backgroundColor                 = Cesium.Color.fromCssColorString('#000005');
    scene.skyBox.show                     = true;
    scene.sun.show                        = true;
    scene.moon.show                       = true;
    scene.skyAtmosphere.show              = true;
    scene.globe.enableLighting            = true;
    scene.globe.atmosphereLightIntensity  = 10.0;
    scene.globe.showGroundAtmosphere      = true;
    scene.logarithmicDepthBuffer          = true;

    this.viewer.scene.screenSpaceCameraController.enableCollisionDetection = false;
  }

  // ── Flights ────────────────────────────────────────────────

  addOrUpdateFlight(dto: AircraftPositionDto): void {
    if (!this.viewer) return;

    const color    = Cesium.Color.fromCssColorString('#0A84FF').withAlpha(0.95);
    const altitude = dto.altitudeMeters ?? 0;
    const position = Cesium.Cartesian3.fromDegrees(dto.lon, dto.lat, altitude);

    if (this.flightEntities.has(dto.icao24)) {
      const entity = this.flightEntities.get(dto.icao24);
      entity.position = position;

      if (dto.headingDeg !== null && dto.headingDeg !== undefined) {
        entity.orientation = Cesium.Transforms.headingPitchRollQuaternion(
          position,
          new Cesium.HeadingPitchRoll(Cesium.Math.toRadians(dto.headingDeg), 0, 0)
        );
      }

      // Update label text if callsign changed
      if (entity.label) {
        entity.label.text = dto.callsign ?? dto.icao24;
      }
    } else {
      const entity = this.viewer.entities.add({
        id:       `flight-${dto.icao24}`,
        name:     dto.callsign ?? dto.icao24,
        position,
        orientation: dto.headingDeg !== null && dto.headingDeg !== undefined
          ? Cesium.Transforms.headingPitchRollQuaternion(
              position,
              new Cesium.HeadingPitchRoll(Cesium.Math.toRadians(dto.headingDeg), 0, 0)
            )
          : undefined,
        point: {
          pixelSize:    6,
          color,
          outlineColor: Cesium.Color.WHITE.withAlpha(0.25),
          outlineWidth: 1,
          heightReference: Cesium.HeightReference.NONE,
          scaleByDistance: new Cesium.NearFarScalar(1e5, 1.6, 2e7, 0.3),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
        label: {
          text:       dto.callsign ?? dto.icao24,
          font:       '11px -apple-system, "SF Pro Text", sans-serif',
          fillColor:  Cesium.Color.WHITE.withAlpha(0.88),
          outlineColor: Cesium.Color.BLACK.withAlpha(0.55),
          outlineWidth: 1,
          style:      Cesium.LabelStyle.FILL_AND_OUTLINE,
          pixelOffset: new Cesium.Cartesian2(0, -16),
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 2.5e6),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          showBackground:     false,
          horizontalOrigin:   Cesium.HorizontalOrigin.CENTER,
          verticalOrigin:     Cesium.VerticalOrigin.BOTTOM,
        },
      });

      this.flightEntities.set(dto.icao24, entity);
    }
  }

  removeFlight(icao24: string): void {
    if (!this.viewer) return;
    const entity = this.flightEntities.get(icao24);
    if (entity) {
      this.viewer.entities.remove(entity);
      this.flightEntities.delete(icao24);
    }
  }

  setFlightsVisible(visible: boolean): void {
    this.flightEntities.forEach(entity => {
      entity.show = visible;
    });
  }

  // ── Ships ──────────────────────────────────────────────────

  addOrUpdateShip(dto: VesselPositionDto): void {
    if (!this.viewer) return;

    const color    = Cesium.Color.fromCssColorString('#30D158').withAlpha(0.95);
    const position = Cesium.Cartesian3.fromDegrees(dto.lon, dto.lat, 0);

    if (this.shipEntities.has(dto.mmsi)) {
      const entity = this.shipEntities.get(dto.mmsi);
      entity.position = position;

      if (dto.courseDeg !== null && dto.courseDeg !== undefined) {
        entity.orientation = Cesium.Transforms.headingPitchRollQuaternion(
          position,
          new Cesium.HeadingPitchRoll(Cesium.Math.toRadians(dto.courseDeg), 0, 0)
        );
      }

      if (entity.label) {
        entity.label.text = dto.name ?? dto.mmsi;
      }
    } else {
      const entity = this.viewer.entities.add({
        id:       `ship-${dto.mmsi}`,
        name:     dto.name ?? dto.mmsi,
        position,
        point: {
          pixelSize:   5,
          color,
          outlineColor: Cesium.Color.WHITE.withAlpha(0.20),
          outlineWidth: 1,
          heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
          scaleByDistance: new Cesium.NearFarScalar(1e4, 1.6, 1e7, 0.3),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
        label: {
          text:       dto.name ?? dto.mmsi,
          font:       '10px -apple-system, "SF Pro Text", sans-serif',
          fillColor:  Cesium.Color.fromCssColorString('#30D158').withAlpha(0.9),
          outlineColor: Cesium.Color.BLACK.withAlpha(0.55),
          outlineWidth: 1,
          style:      Cesium.LabelStyle.FILL_AND_OUTLINE,
          pixelOffset: new Cesium.Cartesian2(0, -14),
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 1.5e6),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
          verticalOrigin:   Cesium.VerticalOrigin.BOTTOM,
        },
      });

      this.shipEntities.set(dto.mmsi, entity);
    }
  }

  removeShip(mmsi: string): void {
    if (!this.viewer) return;
    const entity = this.shipEntities.get(mmsi);
    if (entity) {
      this.viewer.entities.remove(entity);
      this.shipEntities.delete(mmsi);
    }
  }

  setShipsVisible(visible: boolean): void {
    this.shipEntities.forEach(entity => {
      entity.show = visible;
    });
  }

  // ── Satellites ─────────────────────────────────────────────

  addOrUpdateSatellite(dto: SatellitePositionDto): void {
    if (!this.viewer) return;

    const color    = Cesium.Color.fromCssColorString('#FFD60A').withAlpha(0.9);  // Apple yellow
    const altitude = dto.altitudeKm * 1000;  // convert km to meters for Cesium
    const position = Cesium.Cartesian3.fromDegrees(dto.lon, dto.lat, altitude);

    if (this.satelliteEntities.has(dto.noradId)) {
      const entity = this.satelliteEntities.get(dto.noradId);
      entity.position = position;
    } else {
      const entity = this.viewer.entities.add({
        id:       `sat-${dto.noradId}`,
        name:     dto.name ?? dto.noradId,
        position,
        point: {
          pixelSize:    3,
          color,
          outlineColor: Cesium.Color.WHITE.withAlpha(0.2),
          outlineWidth: 1,
          heightReference: Cesium.HeightReference.NONE,
          scaleByDistance: new Cesium.NearFarScalar(1e6, 1.2, 1e8, 0.5),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
        },
      });
      this.satelliteEntities.set(dto.noradId, entity);
    }
  }

  removeSatellite(noradId: string): void {
    if (!this.viewer) return;
    const entity = this.satelliteEntities.get(noradId);
    if (entity) {
      this.viewer.entities.remove(entity);
      this.satelliteEntities.delete(noradId);
    }
  }

  setSatelliteVisibility(visible: boolean): void {
    if (!this.viewer) return;
    this.satelliteEntities.forEach(entity => {
      entity.show = visible;
    });
  }

  // ── Tracks ─────────────────────────────────────────────────

  renderTrack(entityId: string, points: TrackPoint[]): void {
    if (!this.viewer || points.length < 2) return;

    // Remove any existing track for this entity
    const oldTrack = this.trackEntities.get(entityId);
    if (oldTrack) {
      this.viewer.entities.remove(oldTrack);
      this.trackEntities.delete(entityId);
    }

    const positions = points.map(p =>
      Cesium.Cartesian3.fromDegrees(p.lon, p.lat, p.altitudeMeters ?? 0)
    );

    const isAircraft = entityId.startsWith('flight-') || points.some(p => (p.altitudeMeters ?? 0) > 100);

    const trackEntity = this.viewer.entities.add({
      polyline: {
        positions,
        width: 2,
        material: new Cesium.PolylineGlowMaterialProperty({
          glowPower: 0.18,
          taperPower: 0.8,
          color: isAircraft
            ? Cesium.Color.fromCssColorString('#0A84FF').withAlpha(0.75)
            : Cesium.Color.fromCssColorString('#30D158').withAlpha(0.75),
        }),
        clampToGround: !isAircraft,
        arcType: Cesium.ArcType.GEODESIC,
      },
    });

    this.trackEntities.set(entityId, trackEntity);
  }

  clearTracks(): void {
    if (!this.viewer) return;
    this.trackEntities.forEach(entity => this.viewer.entities.remove(entity));
    this.trackEntities.clear();
  }

  clearTrack(entityId: string): void {
    if (!this.viewer) return;
    const entity = this.trackEntities.get(entityId);
    if (entity) {
      this.viewer.entities.remove(entity);
      this.trackEntities.delete(entityId);
    }
  }

  // ── Camera ─────────────────────────────────────────────────

  zoomToFlight(icao24: string): void {
    const entity = this.flightEntities.get(icao24);
    if (entity && this.viewer) {
      this.viewer.flyTo(entity, {
        duration: 1.5,
        offset: new Cesium.HeadingPitchRange(
          0,
          Cesium.Math.toRadians(-30),
          800_000
        ),
      });
    }
  }

  zoomToShip(mmsi: string): void {
    const entity = this.shipEntities.get(mmsi);
    if (entity && this.viewer) {
      this.viewer.flyTo(entity, {
        duration: 1.5,
        offset: new Cesium.HeadingPitchRange(
          0,
          Cesium.Math.toRadians(-45),
          200_000
        ),
      });
    }
  }

  zoomIn(): void {
    if (!this.viewer) return;
    const camera = this.viewer.camera;
    camera.zoomIn(camera.positionCartographic.height * 0.35);
  }

  zoomOut(): void {
    if (!this.viewer) return;
    const camera = this.viewer.camera;
    camera.zoomOut(camera.positionCartographic.height * 0.50);
  }

  zoomToHome(): void {
    if (!this.viewer) return;
    this.viewer.camera.flyHome(1.5);
  }

  // ── Stats ──────────────────────────────────────────────────

  get entityCount(): { flights: number; ships: number; satellites: number } {
    return {
      flights:    this.flightEntities.size,
      ships:      this.shipEntities.size,
      satellites: this.satelliteEntities.size,
    };
  }

  // ── Lifecycle ──────────────────────────────────────────────

  destroy(): void {
    if (this.viewer && !this.viewer.isDestroyed()) {
      this.viewer.destroy();
      this.viewer = null;
    }
    this.flightEntities.clear();
    this.shipEntities.clear();
    this.trackEntities.clear();
    this.satelliteEntities.clear();
  }
}
