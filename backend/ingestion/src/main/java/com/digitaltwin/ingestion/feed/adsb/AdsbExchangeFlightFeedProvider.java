package com.digitaltwin.ingestion.feed.adsb;

import com.digitaltwin.ingestion.feed.FlightFeedProvider;
import com.digitaltwin.shared.event.AircraftRawEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real flight feed provider backed by the ADSBExchange public API.
 *
 * <p>Polls {@code https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json}
 * every 3 seconds. No API key is required. Aircraft on the ground ({@code Gnd=true})
 * and entries without a valid position are skipped.
 *
 * <p>Active when {@code ingestion.feed.flight=adsb}.
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.flight", havingValue = "adsb")
@Slf4j
public class AdsbExchangeFlightFeedProvider implements FlightFeedProvider {

    private static final double FEET_TO_METERS = 0.3048;
    private static final double KNOTS_TO_MPS   = 0.514444;
    private static final double FPM_TO_MPS     = 0.00508;

    private final RestClient restClient;
    private final AdsbExchangeProperties properties;

    public AdsbExchangeFlightFeedProvider(AdsbExchangeProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(properties.getUrl())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AircraftRawEvent> fetch() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("acList")) {
                return List.of();
            }

            List<Map<String, Object>> acList = (List<Map<String, Object>>) response.get("acList");
            if (acList == null) return List.of();

            List<AircraftRawEvent> events = new ArrayList<>(acList.size());
            for (Map<String, Object> ac : acList) {
                AircraftRawEvent event = parseAircraft(ac);
                if (event != null) events.add(event);
            }
            log.debug("Fetched {} aircraft from ADSBExchange", events.size());
            return events;
        } catch (Exception e) {
            log.warn("Failed to fetch from ADSBExchange: {}", e.getMessage());
            return List.of();
        }
    }

    private AircraftRawEvent parseAircraft(Map<String, Object> ac) {
        // Skip aircraft on the ground
        Object gnd = ac.get("Gnd");
        if (Boolean.TRUE.equals(gnd)) return null;

        // Require a valid position — field name is "Long" not "Lon"
        Object latObj = ac.get("Lat");
        Object lonObj = ac.get("Long");
        if (latObj == null || lonObj == null) return null;

        double lat = toDouble(latObj);
        double lon = toDouble(lonObj);

        String icao24   = ac.get("Icao") instanceof String s ? s : null;
        String callsign = ac.get("Call") instanceof String s ? s.strip() : null;

        // Altitude: feet → meters
        Double altitudeMeters = null;
        Object altObj = ac.get("Alt");
        if (altObj instanceof Number n) {
            altitudeMeters = n.doubleValue() * FEET_TO_METERS;
        }

        // Speed: knots → m/s
        Double speedMps = null;
        Object spdObj = ac.get("Spd");
        if (spdObj instanceof Number n) {
            speedMps = n.doubleValue() * KNOTS_TO_MPS;
        }

        // Track / heading: degrees, no conversion needed
        Double headingDeg = null;
        Object trakObj = ac.get("Trak");
        if (trakObj instanceof Number n) {
            headingDeg = n.doubleValue();
        }

        // Vertical speed: feet/min → m/s
        Double verticalRateMps = null;
        Object vsiObj = ac.get("Vsi");
        if (vsiObj instanceof Number n) {
            verticalRateMps = n.doubleValue() * FPM_TO_MPS;
        }

        return new AircraftRawEvent(
                icao24, callsign,
                null,   // airline — not available in this API
                null,   // model   — not available in this API
                null,   // country — not available in this API
                "ADS_B", Instant.now(),
                lat, lon, altitudeMeters, speedMps, headingDeg, verticalRateMps
        );
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(obj));
    }
}
