import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SseService {

  constructor(private zone: NgZone) {}

  /**
   * Opens a Server-Sent Events connection and returns an Observable that emits
   * parsed JSON messages. The connection is automatically closed on unsubscribe.
   *
   * Angular change detection is run via NgZone.run() so that downstream
   * subscribers can safely update component state.
   */
  watch<T>(url: string): Observable<T> {
    return new Observable<T>(observer => {
      const eventSource = new EventSource(url);

      eventSource.onopen = () => {
        // Connection opened — nothing to do here, status is tracked upstream
      };

      eventSource.onmessage = (event: MessageEvent) => {
        try {
          const parsed = JSON.parse(event.data as string) as T;
          this.zone.run(() => observer.next(parsed));
        } catch {
          // Silently ignore malformed JSON frames
        }
      };

      eventSource.onerror = (_event: Event) => {
        this.zone.run(() => observer.error(new Error('SSE connection error')));
        eventSource.close();
      };

      // Teardown: close the EventSource when the subscriber unsubscribes
      return () => {
        eventSource.close();
      };
    });
  }

  /**
   * Listen on a named event channel (e.g. <event type="flight-update">)
   * instead of the default `message` event.
   */
  watchEvent<T>(url: string, eventName: string): Observable<T> {
    return new Observable<T>(observer => {
      const eventSource = new EventSource(url);

      const handler = (event: MessageEvent) => {
        try {
          const parsed = JSON.parse(event.data as string) as T;
          this.zone.run(() => observer.next(parsed));
        } catch {
          // Ignore parse errors
        }
      };

      eventSource.addEventListener(eventName, handler as EventListener);

      eventSource.onerror = () => {
        this.zone.run(() => observer.error(new Error(`SSE error on event "${eventName}"`)));
        eventSource.close();
      };

      return () => {
        eventSource.removeEventListener(eventName, handler as EventListener);
        eventSource.close();
      };
    });
  }
}
