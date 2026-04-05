import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';

export interface LayerToggleEvent {
  layer: string;
  visible: boolean;
}

@Component({
  selector: 'app-layer-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './layer-toggle.component.html',
  styleUrls: ['./layer-toggle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LayerToggleComponent {
  @Input() showFlights    = true;
  @Input() showShips      = true;
  @Input() showSatellites = true;

  @Output() layerToggled = new EventEmitter<LayerToggleEvent>();

  toggle(layer: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.layerToggled.emit({ layer, visible: checked });
  }
}
