import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
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
  connectionStatus: ConnectionStatus = 'connecting';

  selectedFlight: AircraftPositionDto | null = null;
  selectedShip:   VesselPositionDto   | null = null;

  private readonly destroy$ = new Subject<void>();
  private flightSub?:    Subscription;
  private shipSub?:      Subscription;
  private satelliteSub?: Subscription;

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
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.satelliteSub?.unsubscribe();
    this.globe.destroy();
  }

  // ── Streaming ──────────────────────────────────────────────

  private startLiveStreams(): void {
    this.connectionStatus = 'connecting';
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
            // Ships stream error is non-fatal; flights may still be streaming
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
          error: () => { /* satellites optional */ },
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
    }
  }

  // ── Entity selection ───────────────────────────────────────

  closeDetail(): void {
    this.selectedFlight = null;
    this.selectedShip   = null;
    this.globe.clearTracks();
    this.cdr.markForCheck();
  }

  // ── Status helpers ─────────────────────────────────────────

  get statusLabel(): string {
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
}
