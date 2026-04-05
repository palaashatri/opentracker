package com.digitaltwin.ingestion.feed.aishub;

import com.digitaltwin.ingestion.feed.ShipFeedProvider;
import com.digitaltwin.shared.event.ShipRawEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real ship feed provider backed by the AISHub JSON feed.
 *
 * <p>Polls {@code https://data.aishub.net/ws.php} with the configured username.
 * AISHub requires a free account at <a href="https://www.aishub.net">https://www.aishub.net</a>.
 * Supply the username via the {@code AISHUB_USERNAME} environment variable.
 *
 * <p>Vessels at the null island position (0°N, 0°E) are discarded as invalid.
 *
 * <p>Active when {@code ingestion.feed.ship=aishub}.
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.ship", havingValue = "aishub")
@Slf4j
public class AisHubShipFeedProvider implements ShipFeedProvider {

    private final RestClient restClient;
    private final AisHubProperties properties;

    public AisHubShipFeedProvider(AisHubProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ShipRawEvent> fetch() {
        try {
            String uri = UriComponentsBuilder.fromUriString(properties.getUrl())
                    .queryParam("username", properties.getUsername())
                    .queryParam("format", "1")
                    .queryParam("output", "json")
                    .queryParam("compress", "0")
                    .toUriString();

            // AISHub returns a 2-element JSON array:
            // [ { metadata object }, [ { vessel }, ... ] ]
            List<Object> root = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(List.class);

            if (root == null || root.size() < 2) {
                return List.of();
            }

            Object vesselListObj = root.get(1);
            if (!(vesselListObj instanceof List<?> rawList)) {
                return List.of();
            }

            List<ShipRawEvent> events = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> vessel = (Map<String, Object>) rawMap;
                    ShipRawEvent event = parseVessel(vessel);
                    if (event != null) events.add(event);
                }
            }
            log.debug("Fetched {} vessels from AISHub", events.size());
            return events;
        } catch (Exception e) {
            log.warn("Failed to fetch from AISHub: {}", e.getMessage());
            return List.of();
        }
    }

    private ShipRawEvent parseVessel(Map<String, Object> vessel) {
        Object latObj = vessel.get("LAT");
        Object lonObj = vessel.get("LON");
        if (latObj == null || lonObj == null) return null;

        double lat = toDouble(latObj);
        double lon = toDouble(lonObj);

        // Skip null island — both lat AND lon at exactly zero is an invalid default
        if (lat == 0.0 && lon == 0.0) return null;

        String mmsi = vessel.get("MMSI") != null ? String.valueOf(vessel.get("MMSI")) : null;
        String imo  = vessel.get("IMO")  != null ? String.valueOf(vessel.get("IMO"))  : null;

        Object shipNameObj = vessel.get("SHIPNAME");
        String name = shipNameObj instanceof String s ? s.strip() : null;

        int typeCode = vessel.get("SHIPTYPE") instanceof Number n ? n.intValue() : -1;
        String type  = resolveShipType(typeCode);

        String flag = vessel.get("COUNTRY") instanceof String s ? s : null;

        Double speedKnots = vessel.get("SOG") instanceof Number n ? n.doubleValue() : null;
        Double courseDeg  = vessel.get("COG") instanceof Number n ? n.doubleValue() : null;

        return new ShipRawEvent(
                mmsi, imo, name, type, flag,
                "AIS", Instant.now(),
                lat, lon, speedKnots, courseDeg
        );
    }

    /**
     * Maps an AIS vessel-type integer to a human-readable category string.
     *
     * <p>Type ranges follow ITU-R M.1371-5 Table 53.
     */
    private String resolveShipType(int code) {
        if (code >= 20 && code <= 29) return "WIG";
        if (code == 30)               return "FISHING";
        if (code >= 31 && code <= 32) return "TUGBOAT";
        if (code >= 36 && code <= 39) return "PLEASURE";
        if (code >= 40 && code <= 49) return "HIGH_SPEED";
        if (code >= 60 && code <= 69) return "PASSENGER";
        if (code >= 70 && code <= 79) return "CARGO";
        if (code >= 80 && code <= 89) return "TANKER";
        return "OTHER";
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(obj));
    }
}
