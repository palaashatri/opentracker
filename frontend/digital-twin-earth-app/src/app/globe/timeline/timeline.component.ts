import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TimeControlService } from '../../services/time-control.service';
import { PlaybackSpeed } from '../../models/scene.model';

@Component({
  selector: 'app-timeline',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './timeline.component.html',
  styleUrls: ['./timeline.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelineComponent implements OnInit, OnDestroy {

  isLive       = true;
  isPlaying    = false;
  currentSpeed: PlaybackSpeed = 1;
  currentTime: Date = new Date();

  fromValue    = '';
  toValue      = '';

  readonly speedOptions: PlaybackSpeed[] = [1, 2, 5, 10];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly timeControl: TimeControlService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.timeControl.isLive$
      .pipe(takeUntil(this.destroy$))
      .subscribe(live => {
        this.isLive = live;
        this.cdr.markForCheck();
      });

    this.timeControl.isPlaying$
      .pipe(takeUntil(this.destroy$))
      .subscribe(playing => {
        this.isPlaying = playing;
        this.cdr.markForCheck();
      });

    this.timeControl.playbackSpeed$
      .pipe(takeUntil(this.destroy$))
      .subscribe(speed => {
        this.currentSpeed = speed;
        this.cdr.markForCheck();
      });

    this.timeControl.currentTime$
      .pipe(takeUntil(this.destroy$))
      .subscribe(time => {
        this.currentTime = time;
        this.cdr.markForCheck();
      });

    // Set initial date-time input values to last 1 hour window
    const now  = new Date();
    const from = new Date(now.getTime() - 3_600_000);
    this.fromValue = this.toDatetimeLocal(from);
    this.toValue   = this.toDatetimeLocal(now);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Actions ────────────────────────────────────────────────

  goLive(): void {
    this.timeControl.setLive(true);
    const now = new Date();
    this.toValue = this.toDatetimeLocal(now);
  }

  togglePlayback(): void {
    this.timeControl.togglePlayback();
  }

  seekToStart(): void {
    this.timeControl.seekToStart();
  }

  seekToEnd(): void {
    this.timeControl.seekToEnd();
  }

  stepBack(): void {
    const t = new Date(this.currentTime.getTime() - 300_000 * this.currentSpeed);
    this.timeControl.seekTo(t);
  }

  stepForward(): void {
    const t = new Date(this.currentTime.getTime() + 300_000 * this.currentSpeed);
    this.timeControl.seekTo(t);
  }

  setSpeed(speed: PlaybackSpeed): void {
    this.timeControl.setPlaybackSpeed(speed);
  }

  onFromChange(value: string): void {
    this.fromValue = value;
    this.applyHistoricRange();
  }

  onToChange(value: string): void {
    this.toValue = value;
    this.applyHistoricRange();
  }

  private applyHistoricRange(): void {
    const from = new Date(this.fromValue);
    const to   = new Date(this.toValue);
    if (!isNaN(from.getTime()) && !isNaN(to.getTime()) && from < to) {
      this.timeControl.setTimeRange(from, to);
    }
  }

  // ── Scrubber ───────────────────────────────────────────────

  get scrubberPosition(): number {
    const range = this.timeControl.timeRange;
    const total = range.to.getTime() - range.from.getTime();
    if (total <= 0) return 100;
    const elapsed = this.currentTime.getTime() - range.from.getTime();
    return Math.min(100, Math.max(0, (elapsed / total) * 100));
  }

  onScrubberInput(event: Event): void {
    const pct   = Number((event.target as HTMLInputElement).value);
    const range = this.timeControl.timeRange;
    const total = range.to.getTime() - range.from.getTime();
    const target = new Date(range.from.getTime() + (pct / 100) * total);
    this.timeControl.seekTo(target);
  }

  // ── Helpers ────────────────────────────────────────────────

  private toDatetimeLocal(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
