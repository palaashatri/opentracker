package com.digitaltwin.processor.processing;

import com.digitaltwin.processor.cache.PositionCacheService;
import com.digitaltwin.processor.entity.SatelliteEntity;
import com.digitaltwin.processor.entity.SatellitePositionEntity;
import com.digitaltwin.processor.repository.SatellitePositionRepository;
import com.digitaltwin.processor.repository.SatelliteRepository;
import com.digitaltwin.shared.event.SatelliteProcessedEvent;
import com.digitaltwin.shared.event.SatelliteRawEvent;
import com.digitaltwin.shared.kafka.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SatelliteProcessor {

    private final SatelliteRepository satelliteRepository;
    private final SatellitePositionRepository satellitePositionRepository;
    private final PositionCacheService positionCacheService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Integer> updateCounters = new ConcurrentHashMap<>();

    @Transactional
    public void process(SatelliteRawEvent event) {
        // 1. Find or create SatelliteEntity by noradId
        SatelliteEntity satellite = satelliteRepository.findByNoradId(event.noradId())
                .orElseGet(() -> {
                    SatelliteEntity newSatellite = new SatelliteEntity(
                            UUID.randomUUID(),
                            event.noradId(),
                            event.name(),
                            event.source()
                    );
                    return satelliteRepository.save(newSatellite);
                });

        // 2. Build processed event
        SatelliteProcessedEvent processedEvent = new SatelliteProcessedEvent(
                satellite.getId().toString(),
                satellite.getNoradId(),
                satellite.getName(),
                satellite.getSource(),
                event.timestamp(),
                event.lat(),
                event.lon(),
                event.altitudeKm(),
                event.velocityKmS()
        );

        // 3. Cache in Redis with TTL 30 seconds
        positionCacheService.cacheSatellitePosition(event.noradId(), processedEvent);

        // 4. Only write to DB every 60th update per satellite (once per minute)
        int count = updateCounters.merge(event.noradId(), 1, Integer::sum);
        if (count % 60 == 0) {
            SatellitePositionEntity position = new SatellitePositionEntity(
                    UUID.randomUUID(),
                    satellite.getId(),
                    event.timestamp(),
                    event.lat(),
                    event.lon(),
                    event.altitudeKm(),
                    event.velocityKmS()
            );
            satellitePositionRepository.save(position);
            if (count >= 60) {
                updateCounters.put(event.noradId(), 0);
            }
        }

        // 5. Publish processed event to satellites.processed topic
        try {
            String json = objectMapper.writeValueAsString(processedEvent);
            kafkaTemplate.send(KafkaTopics.SATELLITES_PROCESSED, event.noradId(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize processed satellite event for noradId={}", event.noradId(), e);
        }
    }
}
