import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, combineLatest, map } from 'rxjs';
import { PlaybackSpeed } from '../models/scene.model';

export interface TimeRange {
  from: Date;
  to: Date;
}

@Injectable({ providedIn: 'root' })
export class TimeControlService {

  // ── Private state ──────────────────────────────────────────
  private readonly _isLive = new BehaviorSubject<boolean>(true);
  private readonly _timeRange = new BehaviorSubject<TimeRange>({
    from: new Date(Date.now() - 3_600_000),
    to:   new Date(),
  });
  private readonly _playbackSpeed = new BehaviorSubject<PlaybackSpeed>(1);
  private readonly _isPlaying     = new BehaviorSubject<boolean>(false);
  private readonly _currentTime   = new BehaviorSubject<Date>(new Date());

  // ── Public observables ─────────────────────────────────────
  readonly isLive$        = this._isLive.asObservable();
  readonly timeRange$     = this._timeRange.asObservable();
  readonly playbackSpeed$ = this._playbackSpeed.asObservable();
  readonly isPlaying$     = this._isPlaying.asObservable();
  readonly currentTime$   = this._currentTime.asObservable();

  /** Combined state snapshot as a single observable. */
  readonly state$ = combineLatest({
    isLive:        this._isLive,
    timeRange:     this._timeRange,
    playbackSpeed: this._playbackSpeed,
    isPlaying:     this._isPlaying,
    currentTime:   this._currentTime,
  });

  // ── Getters ────────────────────────────────────────────────
  get isLive(): boolean         { return this._isLive.value; }
  get timeRange(): TimeRange    { return this._timeRange.value; }
  get playbackSpeed(): PlaybackSpeed { return this._playbackSpeed.value; }
  get isPlaying(): boolean      { return this._isPlaying.value; }
  get currentTime(): Date       { return this._currentTime.value; }

  // ── Setters / actions ──────────────────────────────────────

  setLive(live: boolean): void {
    this._isLive.next(live);
    if (live) {
      // Return to real time
      this._isPlaying.next(false);
      this._currentTime.next(new Date());
    }
  }

  setTimeRange(from: Date, to: Date): void {
    this._timeRange.next({ from, to });
    this._currentTime.next(from);
    this._isLive.next(false);
  }

  setPlaybackSpeed(speed: PlaybackSpeed): void {
    this._playbackSpeed.next(speed);
  }

  togglePlayback(): void {
    this._isPlaying.next(!this._isPlaying.value);
  }

  play(): void  { this._isPlaying.next(true); }
  pause(): void { this._isPlaying.next(false); }

  seekTo(time: Date): void {
    this._currentTime.next(time);
    this._isLive.next(false);
  }

  /**
   * Step the current time forward by one second scaled by playback speed.
   * Called by the globe component's animation ticker.
   */
  tick(): void {
    if (!this._isPlaying.value || this._isLive.value) return;

    const next = new Date(
      this._currentTime.value.getTime() + 1000 * this._playbackSpeed.value
    );

    if (next >= this._timeRange.value.to) {
      this._currentTime.next(this._timeRange.value.to);
      this._isPlaying.next(false);
    } else {
      this._currentTime.next(next);
    }
  }

  /**
   * Seek to the start of the time range.
   */
  seekToStart(): void {
    this._currentTime.next(this._timeRange.value.from);
    this._isLive.next(false);
  }

  /**
   * Seek to the end of the time range (latest).
   */
  seekToEnd(): void {
    this._currentTime.next(this._timeRange.value.to);
  }
}
