package com.digitaltwin.gateway.controller;

import com.digitaltwin.gateway.sse.SseBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final SseBroadcaster broadcaster;

    public StreamController(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Subscribe to a live stream of flight position updates.
     * Events are pushed whenever a new message arrives on the {@code flights.processed} Kafka topic.
     *
     * @return a long-lived SSE connection
     */
    @GetMapping(value = "/flights", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFlights() {
        return broadcaster.registerFlight();
    }

    /**
     * Subscribe to a live stream of ship position updates.
     * Events are pushed whenever a new message arrives on the {@code ships.processed} Kafka topic.
     *
     * @return a long-lived SSE connection
     */
    @GetMapping(value = "/ships", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamShips() {
        return broadcaster.registerShip();
    }

    /**
     * Subscribe to a live stream of satellite position updates.
     * Events are pushed whenever a new message arrives on the {@code satellites.processed} Kafka topic.
     *
     * @return a long-lived SSE connection
     */
    @GetMapping(value = "/satellites", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSatellites() {
        return broadcaster.registerSatellite();
    }
}
