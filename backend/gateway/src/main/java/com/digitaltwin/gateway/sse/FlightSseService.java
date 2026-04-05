package com.digitaltwin.gateway.sse;

import com.digitaltwin.shared.constants.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FlightSseService {

    private static final Logger log = LoggerFactory.getLogger(FlightSseService.class);

    private final SseBroadcaster broadcaster;

    public FlightSseService(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @KafkaListener(
            topics = KafkaTopics.FLIGHTS_PROCESSED,
            groupId = "gateway-sse-flights",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFlightEvent(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            log.warn("Received blank flight event from Kafka, skipping");
            return;
        }
        log.trace("Broadcasting flight event key={}", record.key());
        broadcaster.broadcastFlight(payload);
    }
}
