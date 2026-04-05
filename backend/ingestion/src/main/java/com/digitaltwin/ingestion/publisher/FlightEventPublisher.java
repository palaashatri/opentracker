package com.digitaltwin.ingestion.publisher;

import com.digitaltwin.shared.constants.KafkaTopics;
import com.digitaltwin.shared.event.AircraftRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlightEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FlightEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FlightEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(AircraftRawEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.FLIGHTS_RAW, event.icao24(), payload);
        } catch (Exception e) {
            log.error("Failed to publish AircraftRawEvent for icao24={}: {}", event.icao24(), e.getMessage(), e);
        }
    }
}
