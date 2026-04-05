package com.digitaltwin.ingestion.feed.satellite;

import com.digitaltwin.ingestion.publisher.SatelliteEventPublisher;
import com.digitaltwin.shared.event.SatelliteRawEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches Two-Line Element (TLE) sets from Celestrak every 3 hours and
 * propagates satellite positions every second using a custom Keplerian propagator.
 *
 * <p>Each propagation cycle publishes a {@link SatelliteRawEvent} per satellite to
 * the {@code satellites.raw} Kafka topic via {@link SatelliteEventPublisher}.
 *
 * <p>Active when {@code ingestion.feed.satellite.enabled=true} (default).
 * Disable by setting {@code SATELLITES_ENABLED=false}.
 */
@Component
@ConditionalOnProperty(name = "ingestion.satellite.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class SatelliteTleProvider {

    private final SatelliteEventPublisher          publisher;
    private final SatelliteTleProperties           properties;
    private final RestClient                       restClient;

    /** NORAD catalogue number → parsed TLE propagator. */
    private final ConcurrentHashMap<String, TlePropagator> propagators = new ConcurrentHashMap<>();

    public SatelliteTleProvider(SatelliteEventPublisher publisher,
                                SatelliteTleProperties properties) {
        this.publisher   = publisher;
        this.properties  = properties;
        this.restClient  = RestClient.builder().build();
    }

    /** Loads TLEs immediately on application startup. */
    @PostConstruct
    public void start() {
        fetchTles();
    }

    /**
     * Refreshes the TLE catalogue from Celestrak every 3 hours (10,800,000 ms).
     *
     * <p>On failure the existing in-memory catalogue continues to be used.
     * TLEs are typically valid for several days, so a missed refresh is not
     * operationally significant.
     */
    @Scheduled(fixedDelay = 10_800_000)
    public void fetchTles() {
        try {
            String tleText = restClient.get()
                    .uri(properties.getTleUrl())
                    .retrieve()
                    .body(String.class);

            if (tleText != null && !tleText.isBlank()) {
                parseTles(tleText);
                log.info("Loaded {} satellites from Celestrak", propagators.size());
            } else {
                log.warn("Celestrak returned an empty TLE response");
            }
        } catch (Exception e) {
            log.error("Failed to fetch TLEs from Celestrak: {}", e.getMessage());
        }
    }

    /**
     * Parses a 3-line-per-satellite TLE text block into the in-memory propagator map.
     *
     * <p>The previous catalogue is replaced atomically so that the propagation
     * thread never observes a partially updated state.
     */
    private void parseTles(String tleText) {
        String[] lines = tleText.split("\\r?\\n");
        Map<String, TlePropagator> fresh = new HashMap<>();

        for (int i = 0; i + 2 < lines.length; i += 3) {
            String name  = lines[i].trim();
            String line1 = lines[i + 1].trim();
            String line2 = lines[i + 2].trim();

            if (!line1.startsWith("1 ") || !line2.startsWith("2 ")) continue;

            TlePropagator p = TlePropagator.parse(name, line1, line2);
            if (p != null) {
                fresh.put(p.noradId, p);
            }
        }

        propagators.clear();
        propagators.putAll(fresh);
    }

    /**
     * Propagates all loaded satellites to the current epoch and publishes their positions.
     *
     * <p>Runs every second. Satellites returning {@code null} from the propagator
     * (invalid orbit, below surface, non-finite coordinates) are skipped silently.
     */
    @Scheduled(fixedDelay = 1_000)
    public void propagate() {
        if (propagators.isEmpty() || !properties.isEnabled()) return;

        long nowMs    = System.currentTimeMillis();
        int  published = 0;

        for (TlePropagator p : propagators.values()) {
            TlePropagator.Position pos = p.propagate(nowMs);
            if (pos == null) continue;

            SatelliteRawEvent event = new SatelliteRawEvent(
                    p.noradId,
                    p.name,
                    "TLE",
                    Instant.ofEpochMilli(nowMs),
                    pos.latDeg,
                    pos.lonDeg,
                    pos.altKm,
                    pos.velocityKmS
            );
            publisher.publish(event);
            published++;
        }
        log.debug("Propagated {} satellite positions", published);
    }
}
