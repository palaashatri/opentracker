package com.digitaltwin.processor.processing;

import com.digitaltwin.processor.cache.PositionCacheService;
import com.digitaltwin.processor.entity.VesselEntity;
import com.digitaltwin.processor.entity.VesselPositionEntity;
import com.digitaltwin.processor.repository.VesselPositionRepository;
import com.digitaltwin.processor.repository.VesselRepository;
import com.digitaltwin.shared.event.ShipProcessedEvent;
import com.digitaltwin.shared.event.ShipRawEvent;
import com.digitaltwin.shared.kafka.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipProcessor {

    private final VesselRepository vesselRepository;
    private final VesselPositionRepository vesselPositionRepository;
    private final PositionCacheService positionCacheService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(ShipRawEvent event) {
        // 1. Find or create VesselEntity by mmsi
        VesselEntity vessel = vesselRepository.findByMmsi(event.mmsi())
                .orElseGet(() -> {
                    VesselEntity newVessel = new VesselEntity(
                            UUID.randomUUID(),
                            event.mmsi(),
                            event.imo(),
                            event.name(),
                            event.type(),
                            event.flag(),
                            event.source()
                    );
                    return vesselRepository.save(newVessel);
                });

        // Update mutable fields if they changed
        boolean updated = false;
        if (event.name() != null && !event.name().equals(vessel.getName())) {
            vessel.setName(event.name());
            updated = true;
        }
        if (event.imo() != null && !event.imo().equals(vessel.getImo())) {
            vessel.setImo(event.imo());
            updated = true;
        }
        if (event.type() != null && !event.type().equals(vessel.getType())) {
            vessel.setType(event.type());
            updated = true;
        }
        if (updated) {
            vesselRepository.save(vessel);
        }

        // 2. Create VesselPositionEntity
        VesselPositionEntity position = new VesselPositionEntity(
                UUID.randomUUID(),
                vessel.getId(),
                event.timestamp(),
                event.lat(),
                event.lon(),
                event.speedKnots(),
                event.courseDeg()
        );

        // 3. Save position
        vesselPositionRepository.save(position);

        // 4. Build processed event
        ShipProcessedEvent processedEvent = new ShipProcessedEvent(
                vessel.getId().toString(),
                vessel.getMmsi(),
                vessel.getImo(),
                vessel.getName(),
                vessel.getType(),
                vessel.getFlag(),
                vessel.getSource(),
                event.timestamp(),
                event.lat(),
                event.lon(),
                event.speedKnots(),
                event.courseDeg()
        );

        // 5. Update Redis cache
        positionCacheService.cacheShipPosition(event.mmsi(), processedEvent);

        // 6. Publish to ships.processed topic
        try {
            String json = objectMapper.writeValueAsString(processedEvent);
            kafkaTemplate.send(KafkaTopics.SHIPS_PROCESSED, event.mmsi(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize processed ship event for mmsi={}", event.mmsi(), e);
        }
    }
}
