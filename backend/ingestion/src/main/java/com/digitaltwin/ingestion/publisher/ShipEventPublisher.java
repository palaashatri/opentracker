package com.digitaltwin.ingestion.publisher;

import com.digitaltwin.shared.constants.KafkaTopics;
import com.digitaltwin.shared.event.ShipRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShipEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ShipEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(ShipRawEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.SHIPS_RAW, event.mmsi(), payload);
        } catch (Exception e) {
            log.error("Failed to publish ShipRawEvent for mmsi={}: {}", event.mmsi(), e.getMessage(), e);
        }
    }
}
