package com.digitaltwin.geospatial.service;

import com.digitaltwin.geospatial.repository.SatellitePositionQueryRepository;
import com.digitaltwin.shared.dto.SatellitePositionDto;
import com.digitaltwin.shared.event.SatelliteProcessedEvent;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SatelliteQueryService {

    private final SatellitePositionQueryRepository queryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<SatellitePositionDto> getAllSatellites() {
        List<SatellitePositionDto> redisResults = queryFromRedis();
        if (!redisResults.isEmpty()) {
            return redisResults;
        }
        return queryFromDb();
    }

    private List<SatellitePositionDto> queryFromRedis() {
        List<SatellitePositionDto> results = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys("satellite:current:*");
            if (keys == null || keys.isEmpty()) {
                return results;
            }
            for (String key : keys) {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) {
                    continue;
                }
                SatelliteProcessedEvent event = objectMapper.readValue(json, SatelliteProcessedEvent.class);
                results.add(new SatellitePositionDto(
                        event.satelliteId(),
                        event.noradId(),
                        event.name(),
                        event.timestamp(),
                        event.lat(),
                        event.lon(),
                        event.altitudeKm(),
                        event.velocityKmS()
                ));
            }
        } catch (Exception e) {
            log.warn("Redis query failed for satellites, falling back to DB", e);
            return new ArrayList<>();
        }
        return results;
    }

    private List<SatellitePositionDto> queryFromDb() {
        List<Object[]> rows = queryRepository.findAllCurrent();
        List<SatellitePositionDto> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(mapToSatellitePositionDto(row));
        }
        return results;
    }

    // Columns: id(0), satellite_id(1), norad_id(2), name(3), recorded_at(4),
    //          lat(5), lon(6), altitude_km(7), velocity_km_s(8)
    private SatellitePositionDto mapToSatellitePositionDto(Object[] row) {
        String id = row[0] != null ? row[0].toString() : null;
        String noradId = (String) row[2];
        String name = (String) row[3];
        Instant recordedAt = toInstant(row[4]);
        double lat = toDouble(row[5]);
        double lon = toDouble(row[6]);
        double altitudeKm = toDouble(row[7]);
        double velocityKmS = toDouble(row[8]);
        return new SatellitePositionDto(id, noradId, name, recordedAt, lat, lon, altitudeKm, velocityKmS);
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
}
