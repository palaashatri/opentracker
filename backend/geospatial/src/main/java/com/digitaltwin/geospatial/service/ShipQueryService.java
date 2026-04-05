package com.digitaltwin.geospatial.service;

import com.digitaltwin.geospatial.repository.VesselPositionQueryRepository;
import com.digitaltwin.shared.dto.TrackDto;
import com.digitaltwin.shared.dto.TrackPointDto;
import com.digitaltwin.shared.dto.VesselPositionDto;
import com.digitaltwin.shared.event.ShipProcessedEvent;
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
public class ShipQueryService {

    private final VesselPositionQueryRepository queryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<VesselPositionDto> getCurrentShips(
            double minLat, double maxLat, double minLon, double maxLon) {

        // Try Redis first
        List<VesselPositionDto> redisResults = queryFromRedis(minLat, maxLat, minLon, maxLon);
        if (!redisResults.isEmpty()) {
            return redisResults;
        }

        // Fall back to DB
        return queryFromDb(minLat, maxLat, minLon, maxLon);
    }

    private List<VesselPositionDto> queryFromRedis(
            double minLat, double maxLat, double minLon, double maxLon) {
        List<VesselPositionDto> results = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys("ship:current:*");
            if (keys == null || keys.isEmpty()) {
                return results;
            }
            for (String key : keys) {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) {
                    continue;
                }
                ShipProcessedEvent event = objectMapper.readValue(json, ShipProcessedEvent.class);
                if (event.lat() >= minLat && event.lat() <= maxLat
                        && event.lon() >= minLon && event.lon() <= maxLon) {
                    results.add(new VesselPositionDto(
                            event.vesselId(),
                            event.vesselId(),
                            event.mmsi(),
                            event.name(),
                            event.timestamp(),
                            event.lat(),
                            event.lon(),
                            event.speedKnots(),
                            event.courseDeg()
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Redis query failed for ships bbox, falling back to DB", e);
            return new ArrayList<>();
        }
        return results;
    }

    private List<VesselPositionDto> queryFromDb(
            double minLat, double maxLat, double minLon, double maxLon) {
        List<Object[]> rows = queryRepository.findCurrentByBbox(minLat, maxLat, minLon, maxLon);
        List<VesselPositionDto> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(mapToVesselPositionDto(row));
        }
        return results;
    }

    public TrackDto getTrack(UUID vesselId, Instant from, Instant to) {
        List<Object[]> rows = queryRepository.findTrack(vesselId, from, to);
        List<TrackPointDto> points = new ArrayList<>();
        for (Object[] row : rows) {
            // Columns: id(0), vessel_id(1), mmsi(2), name(3), recorded_at(4),
            //          lat(5), lon(6), speed_knots(7), course_deg(8)
            Instant timestamp = toInstant(row[4]);
            double lat = toDouble(row[5]);
            double lon = toDouble(row[6]);
            points.add(new TrackPointDto(timestamp, lat, lon, null, toNullableDouble(row[8])));
        }
        return new TrackDto(vesselId.toString(), "vessel", points);
    }

    // Columns: id(0), vessel_id(1), mmsi(2), name(3), recorded_at(4),
    //          lat(5), lon(6), speed_knots(7), course_deg(8)
    private VesselPositionDto mapToVesselPositionDto(Object[] row) {
        String id = row[0] != null ? row[0].toString() : null;
        String vesselId = row[1] != null ? row[1].toString() : null;
        String mmsi = (String) row[2];
        String name = (String) row[3];
        Instant recordedAt = toInstant(row[4]);
        double lat = toDouble(row[5]);
        double lon = toDouble(row[6]);
        Double speedKnots = toNullableDouble(row[7]);
        Double courseDeg = toNullableDouble(row[8]);
        return new VesselPositionDto(id, vesselId, mmsi, name, recordedAt, lat, lon, speedKnots, courseDeg);
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
