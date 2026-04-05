package com.digitaltwin.ingestion.feed.aisstream;

import com.digitaltwin.ingestion.feed.ShipFeedProvider;
import com.digitaltwin.shared.event.ShipRawEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real ship feed provider backed by AISStream.io WebSocket API.
 *
 * <p>Connects to {@code wss://stream.aisstream.io/v0/stream} and subscribes to
 * {@code PositionReport} messages for a global bounding box. Incoming AIS position
 * data is buffered in a {@link CopyOnWriteArrayList} (capped at 1 000 entries);
 * each call to {@link #fetch()} drains and returns the accumulated buffer.
 *
 * <p>A background scheduler checks connection health every 30 seconds and
 * reconnects automatically if the WebSocket has closed or errored.
 *
 * <p>Active when {@code ingestion.feed.ship=aisstream}.
 * Requires {@code AISSTREAM_API_KEY} to be set.
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.ship", havingValue = "aisstream")
@Slf4j
public class AisStreamShipFeedProvider implements ShipFeedProvider {

    private static final String AISSTREAM_URI = "wss://stream.aisstream.io/v0/stream";
    private static final int BUFFER_MAX = 1000;

    private final AisStreamProperties properties;
    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<ShipRawEvent> buffer = new CopyOnWriteArrayList<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile WebSocket webSocket;
    private ScheduledExecutorService reconnectScheduler;

    public AisStreamShipFeedProvider(AisStreamProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        connect();
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "aisstream-reconnect");
            t.setDaemon(true);
            return t;
        });
        reconnectScheduler.scheduleWithFixedDelay(() -> {
            if (!connected.get()) {
                log.info("AISStream: reconnecting...");
                connect();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (reconnectScheduler != null) reconnectScheduler.shutdownNow();
        if (webSocket != null) webSocket.abort();
    }

    private void connect() {
        try {
            webSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(AISSTREAM_URI), new WebSocketListener())
                    .join();
        } catch (Exception e) {
            log.error("AISStream connect failed: {}", e.getMessage());
        }
    }

    /**
     * Returns all ship events accumulated since the last call and clears the buffer.
     */
    @Override
    public List<ShipRawEvent> fetch() {
        if (buffer.isEmpty()) return List.of();
        List<ShipRawEvent> snapshot = new ArrayList<>(buffer);
        buffer.clear();
        return snapshot;
    }

    // -------------------------------------------------------------------------
    // WebSocket listener
    // -------------------------------------------------------------------------

    private class WebSocketListener implements WebSocket.Listener {

        /**
         * Accumulates message fragments when a single logical message is split
         * across multiple {@code onText} frames (WebSocket framing).
         */
        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            connected.set(true);
            log.info("AISStream WebSocket connected");
            String subscribe = String.format(
                    "{\"APIKey\":\"%s\",\"BoundingBoxes\":[[[-90,-180],[90,180]]],\"FilterMessageTypes\":[\"PositionReport\"]}",
                    properties.getApiKey()
            );
            ws.sendText(subscribe, true);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                processMessage(message);
            }
            ws.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            connected.set(false);
            log.warn("AISStream WebSocket error: {}", error.getMessage());
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            connected.set(false);
            log.info("AISStream WebSocket closed: {} {}", statusCode, reason);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Message parsing
    // -------------------------------------------------------------------------

    /**
     * Parses an AISStream {@code PositionReport} JSON message.
     *
     * <p>Expected structure:
     * <pre>
     * {
     *   "MessageType": "PositionReport",
     *   "MetaData": {
     *     "MMSI": "123456789",
     *     "ShipName": "MY VESSEL",
     *     "latitude": 51.5,
     *     "longitude": -0.1
     *   },
     *   "Message": {
     *     "PositionReport": {
     *       "Sog": 12.3,
     *       "Cog": 270.0
     *     }
     *   }
     * }
     * </pre>
     */
    private void processMessage(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            JsonNode meta = root.path("MetaData");
            JsonNode message = root.path("Message").path("PositionReport");

            if (meta.isMissingNode() || message.isMissingNode()) return;

            String mmsi = meta.path("MMSI").asText(null);
            if (mmsi == null || mmsi.isEmpty()) return;

            double lat = meta.path("latitude").asDouble(Double.NaN);
            double lon = meta.path("longitude").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) return;
            // Discard the default invalid position (0°N 0°E — null island)
            if (lat == 0.0 && lon == 0.0) return;

            String shipName = meta.path("ShipName").asText(null);
            if (shipName != null) shipName = shipName.strip();

            double sog = message.path("Sog").asDouble(0); // speed over ground, knots
            double cog = message.path("Cog").asDouble(0); // course over ground, degrees

            // Keep buffer bounded: evict oldest entry when at capacity
            if (buffer.size() >= BUFFER_MAX) {
                buffer.remove(0);
            }

            ShipRawEvent event = new ShipRawEvent(
                    mmsi, null, shipName, null, null,
                    "AIS", Instant.now(),
                    lat, lon, sog, cog
            );
            buffer.add(event);
        } catch (Exception e) {
            log.debug("Failed to parse AISStream message: {}", e.getMessage());
        }
    }
}
