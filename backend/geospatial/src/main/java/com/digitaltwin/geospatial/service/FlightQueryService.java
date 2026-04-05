package com.digitaltwin.geospatial.service;

import com.digitaltwin.geospatial.repository.AircraftPositionQueryRepository;
import com.digitaltwin.shared.dto.AircraftPositionDto;
import com.digitaltwin.shared.dto.TrackDto;
import com.digitaltwin.shared.dto.TrackPointDto;
import com.digitaltwin.shared.event.AircraftProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightQueryService {

    private final AircraftPositionQueryRepository queryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<AircraftPositionDto> getCurrentFlights(
            double minLat, double maxLat, double minLon, double maxLon) {

        // Try Redis first
        List<AircraftPositionDto> redisResults = queryFromRedis(minLat, maxLat, minLon, maxLon);
        if (!redisResults.isEmpty()) {
            return redisResults;
        }

        // Fall back to DB
        return queryFromDb(minLat, maxLat, minLon, maxLon);
    }

    private List<AircraftPositionDto> queryFromRedis(
            double minLat, double maxLat, double minLon, double maxLon) {
        List<AircraftPositionDto> results = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys("flight:current:*");
            if (keys == null || keys.isEmpty()) {
                return results;
            }
            for (String key : keys) {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) {
                    continue;
                }
                AircraftProcessedEvent event = objectMapper.readValue(json, AircraftProcessedEvent.class);
                if (event.lat() >= minLat && event.lat() <= maxLat
                        && event.lon() >= minLon && event.lon() <= maxLon) {
                    results.add(new AircraftPositionDto(
                            event.aircraftId(),
                            event.aircraftId(),
                            event.icao24(),
                            event.callsign(),
                            event.timestamp(),
                            event.lat(),
                            event.lon(),
                            event.altitudeMeters(),
                            event.groundSpeedMps(),
                            event.headingDeg(),
                            event.verticalRateMps()
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Redis query failed for flights bbox, falling back to DB", e);
            return new ArrayList<>();
        }
        return results;
    }

    private List<AircraftPositionDto> queryFromDb(
            double minLat, double maxLat, double minLon, double maxLon) {
        List<Object[]> rows = queryRepository.findCurrentByBbox(minLat, maxLat, minLon, maxLon);
        List<AircraftPositionDto> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(mapToAircraftPositionDto(row));
        }
        return results;
    }

    public TrackDto getTrack(UUID aircraftId, Instant from, Instant to) {
        List<Object[]> rows = queryRepository.findTrack(aircraftId, from, to);
        List<TrackPointDto> points = new ArrayList<>();
        for (Object[] row : rows) {
            // Columns: id(0), aircraft_id(1), icao24(2), callsign(3), recorded_at(4),
            //          lat(5), lon(6), altitude_m(7), ground_speed_mps(8), heading_deg(9), vertical_rate_mps(10)
            Instant timestamp = toInstant(row[4]);
            double lat = toDouble(row[5]);
            double lon = toDouble(row[6]);
            Double altitudeM = toNullableDouble(row[7]);
            Double headingDeg = toNullableDouble(row[9]);
            points.add(new TrackPointDto(timestamp, lat, lon, altitudeM, headingDeg));
        }
        return new TrackDto(aircraftId.toString(), "aircraft", points);
    }

    // Columns: id(0), aircraft_id(1), icao24(2), callsign(3), recorded_at(4),
    //          lat(5), lon(6), altitude_m(7), ground_speed_mps(8), heading_deg(9), vertical_rate_mps(10)
    private AircraftPositionDto mapToAircraftPositionDto(Object[] row) {
        String id = row[0] != null ? row[0].toString() : null;
        String aircraftId = row[1] != null ? row[1].toString() : null;
        String icao24 = (String) row[2];
        String callsign = (String) row[3];
        Instant recordedAt = toInstant(row[4]);
        double lat = toDouble(row[5]);
        double lon = toDouble(row[6]);
        Double altitudeM = toNullableDouble(row[7]);
        Double groundSpeedMps = toNullableDouble(row[8]);
        Double headingDeg = toNullableDouble(row[9]);
        Double verticalRateMps = toNullableDouble(row[10]);
        return new AircraftPositionDto(
                id, aircraftId, icao24, callsign, recordedAt,
                lat, lon, altitudeM, groundSpeedMps, headingDeg, verticalRateMps
        );
    }

    private Instant toInstant(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant();
        }
        return null;
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        return 0.0;
    }

    private Double toNullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        return null;
    }
}
