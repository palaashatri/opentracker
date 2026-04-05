package com.digitaltwin.processor.processing;

import com.digitaltwin.processor.cache.PositionCacheService;
import com.digitaltwin.processor.entity.AircraftEntity;
import com.digitaltwin.processor.entity.AircraftPositionEntity;
import com.digitaltwin.processor.repository.AircraftPositionRepository;
import com.digitaltwin.processor.repository.AircraftRepository;
import com.digitaltwin.shared.event.AircraftProcessedEvent;
import com.digitaltwin.shared.event.AircraftRawEvent;
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
public class FlightProcessor {

    private final AircraftRepository aircraftRepository;
    private final AircraftPositionRepository aircraftPositionRepository;
    private final PositionCacheService positionCacheService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(AircraftRawEvent event) {
        // 1. Find or create AircraftEntity by icao24
        AircraftEntity aircraft = aircraftRepository.findByIcao24(event.icao24())
                .orElseGet(() -> {
                    AircraftEntity newAircraft = new AircraftEntity(
                            UUID.randomUUID(),
                            event.icao24(),
                            event.callsign(),
                            event.airline(),
                            event.model(),
                            event.country(),
                            event.source()
                    );
                    return aircraftRepository.save(newAircraft);
                });

        // Update mutable fields if they changed
        boolean updated = false;
        if (event.callsign() != null && !event.callsign().equals(aircraft.getCallsign())) {
            aircraft.setCallsign(event.callsign());
            updated = true;
        }
        if (event.airline() != null && !event.airline().equals(aircraft.getAirline())) {
            aircraft.setAirline(event.airline());
            updated = true;
        }
        if (event.model() != null && !event.model().equals(aircraft.getModel())) {
            aircraft.setModel(event.model());
            updated = true;
        }
        if (updated) {
            aircraftRepository.save(aircraft);
        }

        // 2. Create AircraftPositionEntity
        AircraftPositionEntity position = new AircraftPositionEntity(
                UUID.randomUUID(),
                aircraft.getId(),
                event.timestamp(),
                event.lat(),
                event.lon(),
                event.altitudeMeters(),
                event.groundSpeedMps(),
                event.headingDeg(),
                event.verticalRateMps()
        );

        // 3. Save position
        aircraftPositionRepository.save(position);

        // 4. Build processed event
        AircraftProcessedEvent processedEvent = new AircraftProcessedEvent(
                aircraft.getId().toString(),
                aircraft.getIcao24(),
                aircraft.getCallsign(),
                aircraft.getAirline(),
                aircraft.getModel(),
                aircraft.getCountry(),
                aircraft.getSource(),
                event.timestamp(),
                event.lat(),
                event.lon(),
                event.altitudeMeters(),
                event.groundSpeedMps(),
                event.headingDeg(),
                event.verticalRateMps()
        );

        // 5. Update Redis cache
        positionCacheService.cacheFlightPosition(event.icao24(), processedEvent);

        // 6. Publish to flights.processed topic
        try {
            String json = objectMapper.writeValueAsString(processedEvent);
            kafkaTemplate.send(KafkaTopics.FLIGHTS_PROCESSED, event.icao24(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize processed flight event for icao24={}", event.icao24(), e);
        }
    }
}
