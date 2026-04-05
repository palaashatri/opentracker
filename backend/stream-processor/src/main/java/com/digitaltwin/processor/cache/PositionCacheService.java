package com.digitaltwin.processor.cache;

import com.digitaltwin.shared.event.AircraftProcessedEvent;
import com.digitaltwin.shared.event.SatelliteProcessedEvent;
import com.digitaltwin.shared.event.ShipProcessedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheFlightPosition(String icao24, AircraftProcessedEvent event) {
        String key = "flight:current:" + icao24;
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(60));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize flight position for icao24={}", icao24, e);
        }
    }

    public void cacheShipPosition(String mmsi, ShipProcessedEvent event) {
        String key = "ship:current:" + mmsi;
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(60));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ship position for mmsi={}", mmsi, e);
        }
    }

    public void cacheSatellitePosition(String noradId, SatelliteProcessedEvent event) {
        String key = "satellite:current:" + noradId;
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(30));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize satellite position for noradId={}", noradId, e);
        }
    }
}
