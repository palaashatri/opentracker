package com.digitaltwin.gateway.sse;

import com.digitaltwin.shared.constants.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipSseService {

    private static final Logger log = LoggerFactory.getLogger(ShipSseService.class);

    private final SseBroadcaster broadcaster;

    public ShipSseService(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @KafkaListener(
            topics = KafkaTopics.SHIPS_PROCESSED,
            groupId = "gateway-sse-ships",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onShipEvent(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            log.warn("Received blank ship event from Kafka, skipping");
            return;
        }
        log.trace("Broadcasting ship event key={}", record.key());
        broadcaster.broadcastShip(payload);
    }
}
