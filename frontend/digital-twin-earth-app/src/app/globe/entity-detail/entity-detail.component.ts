import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { AircraftPositionDto } from '../../models/aircraft.model';
import { VesselPositionDto } from '../../models/vessel.model';
import { GlobeEngineService } from '../../services/globe-engine.service';
import { FlightService } from '../../services/flight.service';
import { ShipService } from '../../services/ship.service';

type EntityKind = 'flight' | 'vessel' | null;

@Component({
  selector: 'app-entity-detail',
  standalone: true,
  imports: [CommonModule, DecimalPipe, DatePipe],
  templateUrl: './entity-detail.component.html',
  styleUrls: ['./entity-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EntityDetailComponent implements OnChanges {
  @Input() flight: AircraftPositionDto | null = null;
  @Input() vessel: VesselPositionDto   | null = null;

  @Output() closed = new EventEmitter<void>();

  kind: EntityKind = null;
  loadingTrack = false;

  constructor(
    private readonly globe:   GlobeEngineService,
    private readonly flights: FlightService,
    private readonly ships:   ShipService,
    private readonly cdr:     ChangeDetectorRef,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['flight'] && this.flight) {
      this.kind = 'flight';
      this.zoomToFlight();
      this.loadFlightTrack();
    } else if (changes['vessel'] && this.vessel) {
      this.kind = 'vessel';
      this.zoomToShip();
      this.loadShipTrack();
    } else if (!this.flight && !this.vessel) {
      this.kind = null;
    }
  }

  close(): void {
    this.closed.emit();
  }

  // ── Helpers ────────────────────────────────────────────────

  get title(): string {
    if (this.flight) {
      return this.flight.callsign ?? this.flight.icao24;
    }
    if (this.vessel) {
      return this.vessel.name ?? this.vessel.mmsi;
    }
    return '';
  }

  get subtitle(): string {
    if (this.flight) return `ICAO24: ${this.flight.icao24}`;
    if (this.vessel) return `MMSI: ${this.vessel.mmsi}`;
    return '';
  }

  get altitudeFt(): number | null {
    if (!this.flight?.altitudeMeters) return null;
    return Math.round(this.flight.altitudeMeters * 3.28084);
  }

  get speedKts(): number | null {
    if (!this.flight?.groundSpeedMps) return null;
    return Math.round(this.flight.groundSpeedMps * 1.94384);
  }

  get verticalFpm(): number | null {
    if (!this.flight?.verticalRateMps) return null;
    return Math.round(this.flight.verticalRateMps * 196.85);
  }

  private zoomToFlight(): void {
    if (this.flight) {
      this.globe.zoomToFlight(this.flight.icao24);
    }
  }

  private zoomToShip(): void {
    if (this.vessel) {
      this.globe.zoomToShip(this.vessel.mmsi);
    }
  }

  private loadFlightTrack(): void {
    if (!this.flight) return;
    this.loadingTrack = true;
    this.cdr.markForCheck();

    const to   = new Date();
    const from = new Date(to.getTime() - 3_600_000);

    this.flights.getTrack(this.flight.id, from, to).subscribe({
      next: track => {
        this.globe.renderTrack(this.flight!.icao24, track.points);
        this.loadingTrack = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingTrack = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadShipTrack(): void {
    if (!this.vessel) return;
    this.loadingTrack = true;
    this.cdr.markForCheck();

    const to   = new Date();
    const from = new Date(to.getTime() - 6 * 3_600_000);

    this.ships.getTrack(this.vessel.mmsi, from, to).subscribe({
      next: track => {
        this.globe.renderTrack(
          this.vessel!.mmsi,
          track.points.map(p => ({
            timestamp: p.timestamp,
            lat: p.lat,
            lon: p.lon,
            altitudeMeters: 0,
            headingDeg: null,
          }))
        );
        this.loadingTrack = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingTrack = false;
        this.cdr.markForCheck();
      },
    });
  }
}
