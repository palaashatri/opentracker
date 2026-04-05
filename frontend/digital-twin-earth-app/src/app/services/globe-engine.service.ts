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
  private readonly weatherEntities   = new Map<string, any>(); // keyed by weather id
  private readonly streetEntities    = new Map<string, any>(); // keyed by osm id

  private terrainEnabled = true;
  private terrainState: 'ready' | 'fallback' = 'fallback';
  private terrainHint = 'Detailed terrain unavailable. Enable an external terrain provider token to unlock high-resolution elevation.';

  private buildingsTileset: any | null = null; // eslint-disable-line @typescript-eslint/no-explicit-any
  private buildingsEnabled = false;
  private buildingsState: 'ready' | 'missing' = 'missing';
  private buildingsHint = '3D buildings are unavailable from the current public tiles source.';
  private streetModeEnabled = false;
  private streetDatasetHint = 'Street-level 3D mode uses OpenStreetMap buildings and water polygons via Overpass.';

  private weatherEnabled = false;
  private weatherHint = 'Weather overlay unavailable. Open-Meteo may be blocked by network or CORS.';

  // ── Initialisation ─────────────────────────────────────────

  init(container: HTMLElement): void {
    // Tell CesiumJS where to find its static assets (workers, imagery, etc.)
    // Must be set before new Cesium.Viewer() is called.
    (window as any)['CESIUM_BASE_URL'] = '/cesium';

    // Opt out of Cesium Ion entirely — all imagery/terrain comes from free, no-key providers.
    // Without this, Cesium makes Ion token requests that silently fail and can block rendering.
    // Empty string explicitly disables Ion. `undefined` causes Cesium to fall back
    // to its internal demo token which silently fails and can block globe rendering.
    Cesium.Ion.defaultAccessToken = '';

    // In CesiumJS 1.103+, use `baseLayer` in the constructor to set the imagery provider.
    // `imageryProvider` is deprecated and ignored. `baseLayer: false` suppresses Ion but
    // can leave the globe black if not followed correctly. Passing the ImageryLayer directly
    // here is the most reliable approach for CesiumJS 1.116.
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
      // Pass OSM as the base layer directly — suppresses Ion default and renders the map.
      baseLayer: new Cesium.ImageryLayer(
        new Cesium.UrlTemplateImageryProvider({
          url:          'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
          credit:       new Cesium.Credit('© OpenStreetMap contributors', false),
          maximumLevel: 19,
        })
      ),
    });

    this.applyTerrainProvider();

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
    // depthTestAgainstTerrain requires a working terrain provider with loaded tiles.
    // With EllipsoidTerrainProvider (no actual elevation), this would cause the globe
    // surface to be invisible. Leave at default (false).
    scene.globe.depthTestAgainstTerrain   = false;

    this.viewer.scene.screenSpaceCameraController.enableCollisionDetection = false;

    // Refresh weather samples when camera movement ends while weather is visible.
    this.viewer.camera.moveEnd.addEventListener(() => {
      if (this.weatherEnabled) {
        void this.refreshWeatherOverlay();
      }

      if (this.streetModeEnabled && this.buildingsEnabled) {
        void this.loadOpenStreetGeometryAroundCamera();
      }
    });
  }

  private applyTerrainProvider(): void {
    if (!this.viewer) return;

    if (!this.terrainEnabled) {
      this.viewer.terrainProvider = new Cesium.EllipsoidTerrainProvider();
      return;
    }

    // We have no Ion token and no paid terrain API key, so always use the ellipsoid.
    // ArcGISTiledElevationTerrainProvider requires authentication — without a key every
    // terrain tile returns 403, causing per-frame loadImage errors and (with
    // depthTestAgainstTerrain=true) making the globe surface invisible.
    this.viewer.terrainProvider = new Cesium.EllipsoidTerrainProvider();
    this.terrainState = 'fallback';
    this.terrainHint = 'Running on flat ellipsoid terrain. Add a Cesium Ion token to enable high-resolution terrain.';
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
          fillColor:  Cesium.Color.WHITE.withAlpha(0.95),
          outlineColor: Cesium.Color.BLACK.withAlpha(0.55),
          outlineWidth: 1,
          style:      Cesium.LabelStyle.FILL_AND_OUTLINE,
          pixelOffset: new Cesium.Cartesian2(0, -16),
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 2.5e6),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          showBackground:     true,
          backgroundColor:    Cesium.Color.BLACK.withAlpha(0.42),
          backgroundPadding:  new Cesium.Cartesian2(6, 3),
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
          fillColor:  Cesium.Color.fromCssColorString('#D8FFE7').withAlpha(0.95),
          outlineColor: Cesium.Color.BLACK.withAlpha(0.55),
          outlineWidth: 1,
          style:      Cesium.LabelStyle.FILL_AND_OUTLINE,
          pixelOffset: new Cesium.Cartesian2(0, -14),
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 1.5e6),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          showBackground: true,
          backgroundColor: Cesium.Color.BLACK.withAlpha(0.45),
          backgroundPadding: new Cesium.Cartesian2(6, 3),
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

  getCameraCenter(): { lat: number; lon: number } | null {
    if (!this.viewer) return null;

    const cartographic = this.viewer.camera.positionCartographic;
    if (!cartographic || !Number.isFinite(cartographic.latitude) || !Number.isFinite(cartographic.longitude)) {
      return null;
    }

    return {
      lat: Cesium.Math.toDegrees(cartographic.latitude),
      lon: Cesium.Math.toDegrees(cartographic.longitude),
    };
  }

  recoverEarthViewIfNeeded(): void {
    if (!this.viewer) return;

    const canvas = this.viewer.scene?.canvas;
    if (!canvas) return;

    const pick = this.viewer.camera.pickEllipsoid(
      new Cesium.Cartesian2(canvas.clientWidth / 2, canvas.clientHeight / 2),
      this.viewer.scene.globe.ellipsoid
    );

    if (pick) {
      return;
    }

    const center = this.getCameraCenter() ?? { lat: 0, lon: 0 };
    this.viewer.camera.flyTo({
      destination: Cesium.Cartesian3.fromDegrees(center.lon, center.lat, 850_000),
      orientation: {
        heading: this.viewer.camera.heading,
        pitch: Cesium.Math.toRadians(-38),
        roll: 0,
      },
      duration: 1.0,
    });
  }

  async setBuildingsVisible(visible: boolean): Promise<void> {
    if (!this.viewer) return;

    this.buildingsEnabled = visible;
    this.streetModeEnabled = this.streetModeEnabled && visible;

    if (!visible) {
      if (this.buildingsTileset) {
        this.buildingsTileset.show = false;
      }
      this.clearStreetEntities();
      return;
    }

    if (this.buildingsTileset) {
      this.buildingsTileset.show = true;
      this.buildingsState = 'ready';
      this.buildingsHint = '3D buildings loaded from public OSM tiles.';
      return;
    }

    if (typeof Cesium.createOsmBuildingsAsync !== 'function') {
      await this.loadOpenStreetGeometryAroundCamera();
      return;
    }

    try {
      const tileset = await Cesium.createOsmBuildingsAsync();
      this.viewer.scene.primitives.add(tileset);
      this.buildingsTileset = tileset;
      this.buildingsState = 'ready';
      this.buildingsHint = '3D buildings loaded from public OSM tiles.';
    } catch {
      await this.loadOpenStreetGeometryAroundCamera();
    }
  }

  enableStreetViewMode(): void {
    void this.enterOpenStreetLevelMode();
  }

  async enterOpenStreetLevelMode(): Promise<void> {
    if (!this.viewer) return;

    const camera = this.viewer.camera;
    const current = camera.positionCartographic;
    const hasValidCoords = Number.isFinite(current.latitude) && Number.isFinite(current.longitude);
    const targetLon = hasValidCoords ? current.longitude : 0;
    const targetLat = hasValidCoords ? current.latitude : 0;
    const lowHeight = Math.min(Math.max(current.height, 20), 80);

    this.streetModeEnabled = true;
    this.buildingsEnabled = true;

    await this.setBuildingsVisible(true);

    camera.flyTo({
      destination: Cesium.Cartesian3.fromRadians(targetLon, targetLat, lowHeight),
      orientation: {
        heading: camera.heading,
        pitch: Cesium.Math.toRadians(-28),
        roll: 0,
      },
      duration: 1.1,
    });

    if (this.viewer.scene?.screenSpaceCameraController) {
      this.viewer.scene.screenSpaceCameraController.enableCollisionDetection = true;
      this.viewer.scene.screenSpaceCameraController.minimumZoomDistance = 2;
      this.viewer.scene.screenSpaceCameraController.maximumZoomDistance = 20_000;
    }
  }

  exitStreetLevelMode(): void {
    this.streetModeEnabled = false;
    if (this.viewer?.scene?.screenSpaceCameraController) {
      this.viewer.scene.screenSpaceCameraController.enableCollisionDetection = false;
      this.viewer.scene.screenSpaceCameraController.minimumZoomDistance = 1;
      this.viewer.scene.screenSpaceCameraController.maximumZoomDistance = Number.POSITIVE_INFINITY;
    }
  }

  setTerrainVisible(visible: boolean): void {
    this.terrainEnabled = visible;
    this.applyTerrainProvider();
  }

  async setWeatherVisible(visible: boolean): Promise<void> {
    this.weatherEnabled = visible;

    if (!visible) {
      this.weatherEntities.forEach((entity) => {
        entity.show = false;
      });
      return;
    }

    await this.refreshWeatherOverlay();
    this.weatherEntities.forEach((entity) => {
      entity.show = true;
    });
  }

  async refreshWeatherOverlay(): Promise<void> {
    if (!this.viewer || !this.weatherEnabled) return;

    const camera = this.viewer.camera.positionCartographic;
    const centerLat = Cesium.Math.toDegrees(camera.latitude);
    const centerLon = Cesium.Math.toDegrees(camera.longitude);

    const samplePoints = [
      { lat: centerLat - 1.8, lon: centerLon - 2.2 },
      { lat: centerLat - 1.8, lon: centerLon },
      { lat: centerLat - 1.8, lon: centerLon + 2.2 },
      { lat: centerLat, lon: centerLon - 2.2 },
      { lat: centerLat, lon: centerLon },
      { lat: centerLat, lon: centerLon + 2.2 },
      { lat: centerLat + 1.8, lon: centerLon - 2.2 },
      { lat: centerLat + 1.8, lon: centerLon },
      { lat: centerLat + 1.8, lon: centerLon + 2.2 },
    ].filter((point) => point.lat >= -80 && point.lat <= 80 && point.lon >= -180 && point.lon <= 180);

    try {
      const responses = await Promise.all(
        samplePoints.map(async (point) => {
          const endpoint = new URL('https://api.open-meteo.com/v1/forecast');
          endpoint.searchParams.set('latitude', point.lat.toFixed(3));
          endpoint.searchParams.set('longitude', point.lon.toFixed(3));
          endpoint.searchParams.set('current', 'temperature_2m,precipitation,wind_speed_10m,wind_direction_10m,cloud_cover');
          endpoint.searchParams.set('timezone', 'UTC');

          const response = await fetch(endpoint.toString());
          if (!response.ok) {
            throw new Error('Weather provider unavailable');
          }

          const payload = await response.json() as {
            current?: {
              wind_speed_10m?: number;
              wind_direction_10m?: number;
              cloud_cover?: number;
              temperature_2m?: number;
              precipitation?: number;
            };
          };

          return {
            lat: point.lat,
            lon: point.lon,
            windSpeed: payload.current?.wind_speed_10m ?? 0,
            windDirection: payload.current?.wind_direction_10m ?? 0,
            cloudCover: payload.current?.cloud_cover ?? 0,
            temperature: payload.current?.temperature_2m ?? 0,
            precipitation: payload.current?.precipitation ?? 0,
          };
        })
      );

      this.weatherHint = 'Weather and wind data loaded from Open-Meteo.';

      this.weatherEntities.forEach((entity) => this.viewer.entities.remove(entity));
      this.weatherEntities.clear();

      responses.forEach((point, index) => {
        const windLengthDeg = Math.min(0.9, 0.12 + point.windSpeed * 0.015);
        const headingRad = Cesium.Math.toRadians(point.windDirection);
        const endLat = point.lat + windLengthDeg * Math.cos(headingRad);
        const endLon = point.lon + windLengthDeg * Math.sin(headingRad);

        const cloudAlpha = Math.min(0.5, Math.max(0.05, point.cloudCover / 220));
        const cloudEntity = this.viewer.entities.add({
          id: `weather-cloud-${index}`,
          position: Cesium.Cartesian3.fromDegrees(point.lon, point.lat, 1400),
          ellipse: {
            semiMajorAxis: 12000,
            semiMinorAxis: 7000,
            material: Cesium.Color.WHITE.withAlpha(cloudAlpha),
            height: 1200,
            outline: false,
          },
        });

        const windEntity = this.viewer.entities.add({
          id: `weather-wind-${index}`,
          polyline: {
            positions: [
              Cesium.Cartesian3.fromDegrees(point.lon, point.lat, 70),
              Cesium.Cartesian3.fromDegrees(endLon, endLat, 70),
            ],
            width: 2,
            material: Cesium.Color.fromCssColorString('#8BD3FF').withAlpha(0.9),
            clampToGround: false,
          },
          label: {
            text: `${Math.round(point.windSpeed)} m/s · ${Math.round(point.temperature)}C · rain ${point.precipitation.toFixed(1)} mm`,
            font: '10px -apple-system, "SF Pro Text", sans-serif',
            fillColor: Cesium.Color.WHITE,
            outlineColor: Cesium.Color.BLACK.withAlpha(0.55),
            outlineWidth: 1,
            style: Cesium.LabelStyle.FILL_AND_OUTLINE,
            showBackground: true,
            backgroundColor: Cesium.Color.BLACK.withAlpha(0.4),
            backgroundPadding: new Cesium.Cartesian2(5, 3),
            pixelOffset: new Cesium.Cartesian2(0, -10),
            horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          },
          position: Cesium.Cartesian3.fromDegrees(endLon, endLat, 90),
        });

        this.weatherEntities.set(cloudEntity.id, cloudEntity);
        this.weatherEntities.set(windEntity.id, windEntity);
      });
    } catch {
      this.weatherHint = 'Open-Meteo weather feed is unreachable. Check internet or CORS policy.';
    }
  }

  private async loadOpenStreetGeometryAroundCamera(): Promise<void> {
    if (!this.viewer || !this.buildingsEnabled) return;

    const cartographic = this.viewer.camera.positionCartographic;
    const centerLat = Cesium.Math.toDegrees(cartographic.latitude);
    const centerLon = Cesium.Math.toDegrees(cartographic.longitude);

    const spanDeg = 0.015;
    const minLat = Math.max(-85, centerLat - spanDeg);
    const maxLat = Math.min(85, centerLat + spanDeg);
    const minLon = Math.max(-180, centerLon - spanDeg);
    const maxLon = Math.min(180, centerLon + spanDeg);

    const query = `[out:json][timeout:25];(way["building"](${minLat},${minLon},${maxLat},${maxLon});way["natural"="water"](${minLat},${minLon},${maxLat},${maxLon});way["waterway"="riverbank"](${minLat},${minLon},${maxLat},${maxLon}););(._;>;);out body;`;

    try {
      const response = await fetch('https://overpass-api.de/api/interpreter', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: `data=${encodeURIComponent(query)}`,
      });

      if (!response.ok) {
        throw new Error('Overpass not reachable');
      }

      const payload = await response.json() as {
        elements: Array<{
          type: 'node' | 'way' | 'relation';
          id: number;
          lat?: number;
          lon?: number;
          nodes?: number[];
          tags?: Record<string, string>;
        }>;
      };

      const nodeIndex = new Map<number, { lat: number; lon: number }>();
      payload.elements.forEach((element) => {
        if (element.type === 'node' && typeof element.lat === 'number' && typeof element.lon === 'number') {
          nodeIndex.set(element.id, { lat: element.lat, lon: element.lon });
        }
      });

      this.clearStreetEntities();

      payload.elements.forEach((element) => {
        if (element.type !== 'way' || !element.nodes || element.nodes.length < 3) return;

        const positions = element.nodes
          .map((nodeId) => nodeIndex.get(nodeId))
          .filter((entry): entry is { lat: number; lon: number } => !!entry)
          .map((entry) => Cesium.Cartesian3.fromDegrees(entry.lon, entry.lat, 0));

        if (positions.length < 3) return;

        const tags = element.tags ?? {};
        const isBuilding = typeof tags['building'] === 'string';
        const isWater = tags['natural'] === 'water' || tags['waterway'] === 'riverbank';

        if (!isBuilding && !isWater) return;

        if (isBuilding) {
          const levels = Number.parseFloat(tags['building:levels'] ?? '');
          const heightMeters = this.parseMeters(tags['height']) ?? (Number.isFinite(levels) ? levels * 3.2 : 14 + Math.random() * 22);

          const entity = this.viewer.entities.add({
            id: `street-building-${element.id}`,
            polygon: {
              hierarchy: new Cesium.PolygonHierarchy(positions),
              material: Cesium.Color.fromCssColorString('#9FB7CC').withAlpha(0.62),
              outline: false,
              height: 0,
              extrudedHeight: heightMeters,
            },
          });

          this.streetEntities.set(entity.id, entity);
        }

        if (isWater) {
          const entity = this.viewer.entities.add({
            id: `street-water-${element.id}`,
            polygon: {
              hierarchy: new Cesium.PolygonHierarchy(positions),
              material: Cesium.Color.fromCssColorString('#3FA0D8').withAlpha(0.34),
              outline: false,
              height: 0,
            },
          });

          this.streetEntities.set(entity.id, entity);
        }
      });

      const buildingCount = Array.from(this.streetEntities.keys()).filter((key) => key.startsWith('street-building-')).length;
      this.buildingsState = buildingCount > 0 ? 'ready' : 'missing';
      this.buildingsHint = buildingCount > 0
        ? `3D street dataset loaded from OpenStreetMap/Overpass (${buildingCount} structures).`
        : 'OpenStreetMap street-level geometry was empty for this area.';
      this.streetDatasetHint = 'Street-level 3D mode is powered by OpenStreetMap + Overpass open datasets.';
    } catch {
      this.buildingsState = 'missing';
      this.buildingsHint = 'OpenStreetMap 3D data request failed. Check connectivity to overpass-api.de.';
    }
  }

  private clearStreetEntities(): void {
    if (!this.viewer) return;
    this.streetEntities.forEach((entity) => {
      this.viewer.entities.remove(entity);
    });
    this.streetEntities.clear();
  }

  private parseMeters(value: string | undefined): number | null {
    if (!value) return null;
    const parsed = Number.parseFloat(value.replace('m', '').trim());
    return Number.isFinite(parsed) ? parsed : null;
  }

  // ── Stats ──────────────────────────────────────────────────

  get entityCount(): { flights: number; ships: number; satellites: number } {
    return {
      flights:    this.flightEntities.size,
      ships:      this.shipEntities.size,
      satellites: this.satelliteEntities.size,
    };
  }

  get layerHints(): { terrain: string; buildings: string; weather: string } {
    return {
      terrain: this.terrainHint,
      buildings: this.buildingsHint,
      weather: this.weatherHint,
    };
  }

  get hasAdvancedTerrain(): boolean {
    return this.terrainState === 'ready';
  }

  get hasBuildings(): boolean {
    return this.buildingsState === 'ready';
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
    this.weatherEntities.clear();
  }
}
