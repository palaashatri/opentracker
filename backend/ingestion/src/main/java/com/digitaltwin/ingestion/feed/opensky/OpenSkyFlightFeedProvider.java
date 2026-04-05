package com.digitaltwin.ingestion.feed.opensky;

import com.digitaltwin.ingestion.feed.FlightFeedProvider;
import com.digitaltwin.shared.event.AircraftRawEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real flight feed provider backed by the OpenSky Network REST API.
 *
 * <p>Fetches live ADS-B state vectors from {@code https://opensky-network.org/api/states/all}.
 * The anonymous rate limit is 400 requests per 10 minutes (roughly 1 per 1.5 s), so the
 * ingestion scheduler interval should be set to at least 30 000 ms when this provider is
 * active (use the {@code realdata} Spring profile or set
 * {@code INGESTION_SIM_INTERVAL_MS=30000}).
 *
 * <p>Active when {@code ingestion.feed.flight=opensky}.
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.flight", havingValue = "opensky")
@Slf4j
public class OpenSkyFlightFeedProvider implements FlightFeedProvider {

    private static final String OPENSKY_URL = "https://opensky-network.org/api/states/all";

    private final RestClient restClient;
    private final OpenSkyProperties properties;

    public OpenSkyFlightFeedProvider(OpenSkyProperties properties) {
        RestClient.Builder builder = RestClient.builder().baseUrl(OPENSKY_URL);
        // Optional basic auth for registered users (increases rate limit)
        if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
            builder.defaultHeaders(headers ->
                    headers.setBasicAuth(properties.getUsername(), properties.getPassword()));
        }
        this.restClient = builder.build();
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AircraftRawEvent> fetch() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("states")) {
                return List.of();
            }

            List<List<Object>> states = (List<List<Object>>) response.get("states");
            if (states == null) return List.of();

            List<AircraftRawEvent> events = new ArrayList<>(states.size());
            for (List<Object> state : states) {
                AircraftRawEvent event = parseState(state);
                if (event != null) events.add(event);
            }
            log.debug("Fetched {} aircraft from OpenSky Network", events.size());
            return events;
        } catch (Exception e) {
            log.error("Failed to fetch from OpenSky Network: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses a single OpenSky state vector array into an {@link AircraftRawEvent}.
     *
     * <p>State vector index layout:
     * <pre>
     *  0  icao24          (String)
     *  1  callsign        (String, nullable, may have trailing spaces)
     *  2  origin_country  (String)
     *  3  time_position   (Integer, nullable)
     *  4  last_contact    (Integer)
     *  5  longitude       (Double, nullable) — longitude first!
     *  6  latitude        (Double, nullable)
     *  7  baro_altitude   (Double, nullable)
     *  8  on_ground       (Boolean)
     *  9  velocity        (Double, nullable) — m/s
     * 10  true_track      (Double, nullable) — degrees
     * 11  vertical_rate   (Double, nullable) — m/s
     * 12  sensors         (List, nullable)
     * 13  geo_altitude    (Double, nullable)
     * 14  squawk          (String, nullable)
     * 15  spi             (Boolean)
     * 16  position_source (Integer)
     * </pre>
     */
    private AircraftRawEvent parseState(List<Object> state) {
        if (state == null || state.size() < 11) return null;

        // Skip aircraft on ground
        Object onGround = state.get(8);
        if (Boolean.TRUE.equals(onGround)) return null;

        // Longitude is index 5, latitude is index 6
        Object lonObj = state.get(5);
        Object latObj = state.get(6);
        if (lonObj == null || latObj == null) return null;

        String icao24 = String.valueOf(state.get(0));
        Object callsignObj = state.get(1);
        String callsign = callsignObj != null ? String.valueOf(callsignObj).strip() : null;
        String country = state.get(2) != null ? String.valueOf(state.get(2)) : null;

        double lon = toDouble(lonObj);
        double lat = toDouble(latObj);

        // Prefer baro altitude; fall back to geometric altitude
        Double altitude = state.get(7) != null ? toDouble(state.get(7))
                : (state.size() > 13 && state.get(13) != null ? toDouble(state.get(13)) : null);
        Double velocity = state.get(9) != null ? toDouble(state.get(9)) : null;
        Double heading  = state.get(10) != null ? toDouble(state.get(10)) : null;
        Double vertRate = state.get(11) != null ? toDouble(state.get(11)) : null;

        return new AircraftRawEvent(
                icao24, callsign, null, null, country,
                "ADS_B", Instant.now(),
                lat, lon, altitude, velocity, heading, vertRate
        );
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(obj));
    }
}
