import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ConnectionStatus } from '../../models/scene.model';

@Component({
  selector: 'app-stats-panel',
  standalone: true,
  imports: [CommonModule, DecimalPipe, DatePipe],
  templateUrl: './stats-panel.component.html',
  styleUrls: ['./stats-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatsPanelComponent implements OnChanges {
  @Input() flightCount    = 0;
  @Input() shipCount      = 0;
  @Input() satelliteCount = 0;
  @Input() connectionStatus: ConnectionStatus = 'connecting';

  lastUpdated: Date | null = null;
  totalEntities             = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['flightCount'] || changes['shipCount'] || changes['satelliteCount']) {
      this.totalEntities = this.flightCount + this.shipCount + this.satelliteCount;
      if (this.flightCount > 0 || this.shipCount > 0 || this.satelliteCount > 0) {
        this.lastUpdated = new Date();
      }
    }
  }

  get uptimeLabel(): string {
    return this.connectionStatus === 'connected' ? 'Streaming' : 'Offline';
  }
}
