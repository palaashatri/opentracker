package com.digitaltwin.ingestion.publisher;

import com.digitaltwin.shared.constants.KafkaTopics;
import com.digitaltwin.shared.event.SatelliteRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link SatelliteRawEvent} instances to the {@code satellites.raw} Kafka topic,
 * keyed by NORAD catalogue number.
 */
@Component
public class SatelliteEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SatelliteEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SatelliteEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    public void publish(SatelliteRawEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.SATELLITES_RAW, event.noradId(), payload);
        } catch (Exception e) {
            log.error("Failed to publish SatelliteRawEvent for noradId={}: {}", event.noradId(), e.getMessage(), e);
        }
    }
}
