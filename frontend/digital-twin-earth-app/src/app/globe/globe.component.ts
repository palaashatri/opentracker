import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  HostListener,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Subject, Subscription, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { GlobeEngineService } from '../services/globe-engine.service';
import { FlightService } from '../services/flight.service';
import { ShipService } from '../services/ship.service';
import { SatelliteService } from '../services/satellite.service';
import { TimeControlService } from '../services/time-control.service';
import { AircraftPositionDto } from '../models/aircraft.model';
import { VesselPositionDto } from '../models/vessel.model';
import { SatellitePositionDto } from '../models/satellite.model';
import { ConnectionStatus } from '../models/scene.model';

import { LayerToggleComponent } from './layer-toggle/layer-toggle.component';
import { TimelineComponent } from './timeline/timeline.component';
import { StatsPanelComponent } from './stats-panel/stats-panel.component';
import { EntityDetailComponent } from './entity-detail/entity-detail.component';

type StreetPhoto = {
  title: string;
  thumbUrl: string;
  pageUrl: string;
  source: 'Wikimedia Commons';
};

@Component({
  selector: 'app-globe',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    LayerToggleComponent,
    TimelineComponent,
    StatsPanelComponent,
    EntityDetailComponent,
  ],
  templateUrl: './globe.component.html',
  styleUrls: ['./globe.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlobeComponent implements OnInit, OnDestroy {
  @ViewChild('cesiumContainer', { static: true })
  containerRef!: ElementRef<HTMLDivElement>;

  // ── UI state ───────────────────────────────────────────────
  flightCount       = 0;
  shipCount         = 0;
  satelliteCount    = 0;
  isLoading         = true;
  showFlights       = true;
  showShips         = true;
  showSatellites    = true;
  showTerrain       = true;
  showBuildings     = false;
  showWeather       = false;
  connectionStatus: ConnectionStatus = 'connecting';
  isStreetView = false;
  usingMockData = false;
  connectionIssueHint = '';
  isLoadingStreetPhotos = false;
  streetPhotos: StreetPhoto[] = [];

  selectedFlight:    AircraftPositionDto    | null = null;
  selectedShip:      VesselPositionDto      | null = null;
  selectedSatellite: SatellitePositionDto   | null = null;

  searchOpen    = false;
  searchQuery   = '';
  searchResults: Array<{ label: string; sub: string; action: () => void }> = [];
  searchLoading = false;

  private readonly destroy$ = new Subject<void>();
  private flightSub?:    Subscription;
  private shipSub?:      Subscription;
  private satelliteSub?: Subscription;
  private mockSub?:      Subscription;
  private weatherSub?:   Subscription;
  private streetPhotoSub?: Subscription;
  private cameraRecoverySub?: Subscription;

  private readonly mockFlights = new Map<string, {
    lat: number;
    lon: number;
    heading: number;
    speedMps: number;
    altitudeMeters: number;
    callsign: string;
  }>();

  private readonly mockShips = new Map<string, {
    lat: number;
    lon: number;
    heading: number;
    speedKnots: number;
    name: string;
  }>();

  constructor(
    public readonly globe:             GlobeEngineService,
    private readonly flightService:    FlightService,
    private readonly shipService:      ShipService,
    private readonly satelliteService: SatelliteService,
    private readonly timeControl:      TimeControlService,
    private readonly cdr:              ChangeDetectorRef,
  ) {}

  // ── Lifecycle ──────────────────────────────────────────────

  ngOnInit(): void {
    this.globe.init(this.containerRef.nativeElement);
    this.isLoading = false;
    this.cdr.markForCheck();

    this.startLiveStreams();

    // Refresh HUD counts every 5 s without hammering change detection
    interval(5_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        const counts = this.globe.entityCount;
        this.flightCount    = counts.flights;
        this.shipCount      = counts.ships;
        this.satelliteCount = counts.satellites;
        this.cdr.markForCheck();
      });

    this.cameraRecoverySub = interval(3_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.globe.recoverEarthViewIfNeeded();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stopStreams();
    this.mockSub?.unsubscribe();
    this.weatherSub?.unsubscribe();
    this.streetPhotoSub?.unsubscribe();
    this.cameraRecoverySub?.unsubscribe();
    this.satelliteSub?.unsubscribe();
    this.globe.destroy();
  }

  // ── Streaming ──────────────────────────────────────────────

  private startLiveStreams(): void {
    this.connectionStatus = 'connecting';
    this.connectionIssueHint = '';
    this.cdr.markForCheck();

    if (this.showFlights) {
      this.flightSub = this.flightService
        .streamFlights()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (dto: AircraftPositionDto) => {
            this.globe.addOrUpdateFlight(dto);
            this.flightCount      = this.globe.entityCount.flights;
            this.connectionStatus = 'connected';
            this.cdr.markForCheck();
          },
          error: () => {
            this.connectionStatus = 'error';
            this.connectionIssueHint = 'Live flight stream is unavailable. Ensure backend gateway is running at /api and SSE endpoint /api/stream/flights is reachable.';
            this.enableMockTraffic();
            this.cdr.markForCheck();
          },
        });
    }

    if (this.showShips) {
      this.shipSub = this.shipService
        .streamShips()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (dto: VesselPositionDto) => {
            this.globe.addOrUpdateShip(dto);
            this.shipCount = this.globe.entityCount.ships;
            this.cdr.markForCheck();
          },
          error: () => {
            this.connectionIssueHint = this.connectionIssueHint ||
              'Live vessel stream is unavailable. Verify /api/stream/vessels or start backend ingestion services.';
            this.enableMockTraffic();
          },
        });
    }

    if (this.showSatellites) {
      this.satelliteSub = this.satelliteService.streamSatellites()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (dto: SatellitePositionDto) => {
            this.globe.addOrUpdateSatellite(dto);
            this.satelliteCount = this.globe.entityCount.satellites;
            this.cdr.markForCheck();
          },
          error: () => {
            this.connectionIssueHint = this.connectionIssueHint ||
              'Satellite stream is unavailable. This layer is optional and can remain disabled.';
          },
        });
    }
  }

  private stopStreams(): void {
    this.flightSub?.unsubscribe();
    this.shipSub?.unsubscribe();
    this.satelliteSub?.unsubscribe();
  }

  // ── Layer toggle ───────────────────────────────────────────

  onLayerToggle(event: { layer: string; visible: boolean }): void {
    if (event.layer === 'flights') {
      this.showFlights = event.visible;
      this.globe.setFlightsVisible(event.visible);
    } else if (event.layer === 'ships') {
      this.showShips = event.visible;
      this.globe.setShipsVisible(event.visible);
    } else if (event.layer === 'satellites') {
      this.showSatellites = event.visible;
      this.globe.setSatelliteVisibility(event.visible);
    } else if (event.layer === 'terrain') {
      this.showTerrain = event.visible;
      this.globe.setTerrainVisible(event.visible);
    } else if (event.layer === 'buildings') {
      this.showBuildings = event.visible;
      void this.globe.setBuildingsVisible(event.visible).then(() => {
        if (event.visible && !this.globe.hasBuildings) {
          this.connectionIssueHint = this.globe.layerHints.buildings;
        }
        this.cdr.markForCheck();
      });
    } else if (event.layer === 'weather') {
      this.showWeather = event.visible;
      void this.globe.setWeatherVisible(event.visible).then(() => {
        if (event.visible) {
          this.connectionIssueHint = this.globe.layerHints.weather;
        }
        this.cdr.markForCheck();
      });

      if (event.visible) {
        this.weatherSub?.unsubscribe();
        this.weatherSub = interval(180_000)
          .pipe(takeUntil(this.destroy$))
          .subscribe(() => {
            void this.globe.refreshWeatherOverlay();
          });
      } else {
        this.weatherSub?.unsubscribe();
      }
    }

    this.cdr.markForCheck();
  }

  toggle3dView(): void {
    this.isStreetView = !this.isStreetView;
    if (this.isStreetView) {
      void this.globe.enterOpenStreetLevelMode().then(() => {
        this.connectionIssueHint = this.globe.layerHints.buildings;
        void this.loadStreetPhotos();
        this.cdr.markForCheck();
      });
      if (!this.showBuildings) {
        this.showBuildings = true;
        void this.globe.setBuildingsVisible(true).then(() => {
          this.connectionIssueHint = this.globe.layerHints.buildings;
          this.cdr.markForCheck();
        });
      }

      this.streetPhotoSub?.unsubscribe();
      this.streetPhotoSub = interval(120_000)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          void this.loadStreetPhotos();
        });
    } else {
      this.globe.exitStreetLevelMode();
      this.globe.zoomToHome();
      this.streetPhotoSub?.unsubscribe();
    }
    this.cdr.markForCheck();
  }

  // ── Entity selection ───────────────────────────────────────

  closeDetail(): void {
    this.selectedFlight = null;
    this.selectedShip   = null;
    if (this.selectedSatellite) {
      this.globe.clearSatelliteFootprint(this.selectedSatellite.noradId);
      this.selectedSatellite = null;
    }
    this.globe.clearTracks();
    this.cdr.markForCheck();
  }

  selectSatellite(dto: SatellitePositionDto | null): void {
    if (this.selectedSatellite) {
      this.globe.clearSatelliteFootprint(this.selectedSatellite.noradId);
    }
    this.selectedSatellite = dto;
    if (dto) {
      this.globe.showSatelliteFootprint(dto.noradId, dto.lat, dto.lon, dto.altitudeKm);
    }
    this.cdr.markForCheck();
  }

  // ── Keyboard shortcuts ─────────────────────────────────────────────────
  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) return;

    switch (event.key.toLowerCase()) {
      case 'f':
        this.onLayerToggle({ layer: 'flights', visible: !this.showFlights });
        break;
      case 's':
        this.onLayerToggle({ layer: 'ships', visible: !this.showShips });
        break;
      case 't':
        this.onLayerToggle({ layer: 'satellites', visible: !this.showSatellites });
        break;
      case 'h':
        this.globe.zoomToHome();
        break;
      case 'r':
        this.globe.recoverEarthViewIfNeeded();
        break;
      case '+':
      case '=':
        this.globe.zoomIn();
        break;
      case '-':
        this.globe.zoomOut();
        break;
      case '/':
        event.preventDefault();
        this.searchOpen = true;
        this.cdr.markForCheck();
        setTimeout(() => document.getElementById('globe-search')?.focus(), 50);
        break;
      case 'escape':
        if (this.searchOpen) {
          this.searchOpen    = false;
          this.searchQuery   = '';
          this.searchResults = [];
          this.cdr.markForCheck();
        } else if (this.selectedFlight || this.selectedShip || this.selectedSatellite) {
          this.closeDetail();
          this.selectSatellite(null);
        }
        break;
    }
  }

  // ── Search ─────────────────────────────────────────────────

  toggleSearch(): void {
    this.searchOpen = !this.searchOpen;
    this.cdr.markForCheck();
  }

  onSearchInput(query: string): void {
    this.searchQuery = query;
    if (!query.trim()) {
      this.searchResults = [];
      this.cdr.markForCheck();
      return;
    }
    const q = query.trim().toLowerCase();

    // Local entity search first
    const results: Array<{ label: string; sub: string; action: () => void }> = [];
    this.globe.searchEntities(q).forEach(hit => results.push(hit));

    if (results.length > 0) {
      this.searchResults = results.slice(0, 8);
      this.cdr.markForCheck();
      return;
    }

    // Nominatim geocoding fallback
    this.searchLoading = true;
    this.cdr.markForCheck();
    fetch(`https://nominatim.openstreetmap.org/search?format=json&limit=5&q=${encodeURIComponent(query)}`, {
      headers: { 'Accept-Language': 'en', 'User-Agent': 'OpenTracker/1.0' },
    })
      .then(r => r.json())
      .then((items: any[]) => {
        this.searchResults = items.map((item: any) => ({
          label: item.display_name?.split(',')[0] ?? item.display_name,
          sub:   item.display_name,
          action: () => {
            this.globe.flyTo(parseFloat(item.lat), parseFloat(item.lon), 200_000);
            this.closeSearch();
          },
        }));
        this.searchLoading = false;
        this.cdr.markForCheck();
      })
      .catch(() => {
        this.searchLoading = false;
        this.cdr.markForCheck();
      });
  }

  closeSearch(): void {
    this.searchOpen    = false;
    this.searchQuery   = '';
    this.searchResults = [];
    this.cdr.markForCheck();
  }

  // ── Status helpers ─────────────────────────────────────────

  get statusLabel(): string {
    if (this.usingMockData) {
      return 'Mock Live';
    }

    switch (this.connectionStatus) {
      case 'connected':    return 'Live';
      case 'connecting':   return 'Connecting…';
      case 'error':        return 'Connection Error';
      case 'disconnected': return 'Disconnected';
    }
  }

  get currentYear(): number {
    return new Date().getFullYear();
  }

  get statusTooltip(): string {
    if (this.connectionStatus === 'error' || this.usingMockData) {
      return this.connectionIssueHint ||
        'Connection issue detected. Make sure backend services are running or continue with built-in mock traffic mode.';
    }

    if (!this.globe.hasAdvancedTerrain && this.showTerrain) {
      return this.globe.layerHints.terrain;
    }

    if (this.isStreetView && this.showBuildings) {
      return this.globe.layerHints.buildings;
    }

    return 'Live telemetry stream connected.';
  }

  private enableMockTraffic(): void {
    if (this.usingMockData || this.mockSub) {
      return;
    }

    this.seedMockTraffic();
    this.usingMockData = true;
    this.connectionStatus = 'connected';

    this.mockSub = interval(1_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.tickMockTraffic();
        this.cdr.markForCheck();
      });
  }

  private seedMockTraffic(): void {
    if (this.mockFlights.size > 0 || this.mockShips.size > 0) {
      return;
    }

    const flightSeeds = [
      { id: 'AAL101', lat: 37.8, lon: -122.2, heading: 60, speedMps: 235, altitude: 10800 },
      { id: 'BAW782', lat: 51.1, lon: -0.2, heading: 95, speedMps: 248, altitude: 11100 },
      { id: 'DAL454', lat: 40.7, lon: -73.6, heading: 210, speedMps: 240, altitude: 10200 },
      { id: 'UAE225', lat: 25.1, lon: 55.4, heading: 132, speedMps: 252, altitude: 11300 },
      { id: 'SIA012', lat: 1.2, lon: 103.8, heading: 315, speedMps: 245, altitude: 10600 },
      { id: 'AFR350', lat: 48.9, lon: 2.4, heading: 250, speedMps: 230, altitude: 9800 },
    ];

    const shipSeeds = [
      { id: '367001001', name: 'Pacific Horizon', lat: 36.9, lon: -122.8, heading: 122, speed: 15 },
      { id: '366445000', name: 'Arctic Relay', lat: 1.5, lon: 104.1, heading: 87, speed: 17 },
      { id: '235114221', name: 'Blue Meridian', lat: 50.6, lon: 1.4, heading: 245, speed: 12 },
      { id: '538009111', name: 'Ocean Link', lat: 25.4, lon: 56.0, heading: 178, speed: 14 },
    ];

    flightSeeds.forEach((seed, idx) => {
      this.mockFlights.set(seed.id, {
        lat: seed.lat,
        lon: seed.lon,
        heading: seed.heading,
        speedMps: seed.speedMps,
        altitudeMeters: seed.altitude,
        callsign: seed.id,
      });

      const dto: AircraftPositionDto = {
        id: `mock-flight-${idx}`,
        aircraftId: `mock-aircraft-${idx}`,
        icao24: seed.id,
        callsign: seed.id,
        timestamp: new Date().toISOString(),
        lat: seed.lat,
        lon: seed.lon,
        altitudeMeters: seed.altitude,
        groundSpeedMps: seed.speedMps,
        headingDeg: seed.heading,
        verticalRateMps: 0,
      };
      this.globe.addOrUpdateFlight(dto);
    });

    shipSeeds.forEach((seed, idx) => {
      this.mockShips.set(seed.id, {
        lat: seed.lat,
        lon: seed.lon,
        heading: seed.heading,
        speedKnots: seed.speed,
        name: seed.name,
      });

      const dto: VesselPositionDto = {
        id: `mock-ship-${idx}`,
        vesselId: `mock-vessel-${idx}`,
        mmsi: seed.id,
        name: seed.name,
        timestamp: new Date().toISOString(),
        lat: seed.lat,
        lon: seed.lon,
        speedKnots: seed.speed,
        courseDeg: seed.heading,
      };
      this.globe.addOrUpdateShip(dto);
    });
  }

  private tickMockTraffic(): void {
    const now = new Date().toISOString();
    let flightIndex = 0;
    let shipIndex = 0;

    this.mockFlights.forEach((state, id) => {
      state.heading = (state.heading + (flightIndex % 2 === 0 ? 1.4 : -1.1) + 360) % 360;
      const radians = (state.heading * Math.PI) / 180;
      state.lat += Math.cos(radians) * 0.03;
      state.lon += Math.sin(radians) * 0.05;

      if (state.lon > 179.5) state.lon = -179.5;
      if (state.lon < -179.5) state.lon = 179.5;
      state.lat = Math.max(-70, Math.min(70, state.lat));

      const dto: AircraftPositionDto = {
        id: `mock-flight-${flightIndex}`,
        aircraftId: `mock-aircraft-${flightIndex}`,
        icao24: id,
        callsign: state.callsign,
        timestamp: now,
        lat: state.lat,
        lon: state.lon,
        altitudeMeters: state.altitudeMeters,
        groundSpeedMps: state.speedMps,
        headingDeg: state.heading,
        verticalRateMps: Math.sin(Date.now() / 8000) * 0.2,
      };

      this.globe.addOrUpdateFlight(dto);
      flightIndex += 1;
    });

    this.mockShips.forEach((state, id) => {
      state.heading = (state.heading + (shipIndex % 2 === 0 ? 0.5 : -0.35) + 360) % 360;
      const radians = (state.heading * Math.PI) / 180;
      state.lat += Math.cos(radians) * 0.007;
      state.lon += Math.sin(radians) * 0.012;

      if (state.lon > 179.5) state.lon = -179.5;
      if (state.lon < -179.5) state.lon = 179.5;
      state.lat = Math.max(-75, Math.min(75, state.lat));

      const dto: VesselPositionDto = {
        id: `mock-ship-${shipIndex}`,
        vesselId: `mock-vessel-${shipIndex}`,
        mmsi: id,
        name: state.name,
        timestamp: now,
        lat: state.lat,
        lon: state.lon,
        speedKnots: state.speedKnots,
        courseDeg: state.heading,
      };

      this.globe.addOrUpdateShip(dto);
      shipIndex += 1;
    });

    const counts = this.globe.entityCount;
    this.flightCount = counts.flights;
    this.shipCount = counts.ships;
  }

  private async loadStreetPhotos(): Promise<void> {
    if (!this.isStreetView) {
      return;
    }

    const center = this.globe.getCameraCenter();
    if (!center) {
      return;
    }

    this.isLoadingStreetPhotos = true;
    this.cdr.markForCheck();

    try {
      const endpoint = new URL('https://commons.wikimedia.org/w/api.php');
      endpoint.searchParams.set('action', 'query');
      endpoint.searchParams.set('format', 'json');
      endpoint.searchParams.set('generator', 'geosearch');
      endpoint.searchParams.set('ggscoord', `${center.lat}|${center.lon}`);
      endpoint.searchParams.set('ggsradius', '2000');
      endpoint.searchParams.set('ggslimit', '8');
      endpoint.searchParams.set('ggsnamespace', '6');
      endpoint.searchParams.set('prop', 'imageinfo');
      endpoint.searchParams.set('iiprop', 'url');
      endpoint.searchParams.set('iiurlwidth', '360');
      endpoint.searchParams.set('origin', '*');

      const response = await fetch(endpoint.toString());
      if (!response.ok) {
        throw new Error('Photo provider unavailable');
      }

      const payload = await response.json() as {
        query?: {
          pages?: Record<string, {
            title: string;
            imageinfo?: Array<{
              thumburl?: string;
              descriptionurl?: string;
              url?: string;
            }>;
          }>;
        };
      };

      const pages = Object.values(payload.query?.pages ?? {});
      this.streetPhotos = pages
        .map((page) => {
          const imageInfo = page.imageinfo?.[0];
          const thumb = imageInfo?.thumburl ?? imageInfo?.url;
          const pageUrl = imageInfo?.descriptionurl ?? imageInfo?.url;

          if (!thumb || !pageUrl) {
            return null;
          }

          return {
            title: page.title.replace(/^File:/, ''),
            thumbUrl: thumb,
            pageUrl,
            source: 'Wikimedia Commons' as const,
          };
        })
        .filter((photo): photo is StreetPhoto => !!photo)
        .slice(0, 6);

      if (this.streetPhotos.length === 0) {
        this.connectionIssueHint = 'No nearby open street photos found for this area. Move to a denser urban region for better coverage.';
      }
    } catch {
      this.connectionIssueHint = 'Unable to load open street photos from Wikimedia Commons.';
    } finally {
      this.isLoadingStreetPhotos = false;
      this.cdr.markForCheck();
    }
  }
}
