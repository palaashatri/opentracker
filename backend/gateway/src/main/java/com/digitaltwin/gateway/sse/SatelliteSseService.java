package com.digitaltwin.gateway.sse;

import com.digitaltwin.shared.constants.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SatelliteSseService {

    private final SseBroadcaster broadcaster;

    @KafkaListener(
            topics = KafkaTopics.SATELLITES_PROCESSED,
            groupId = "gateway-sse-satellites",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSatelliteEvent(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null || payload.isBlank()) {
            log.warn("Received blank satellite event from Kafka, skipping");
            return;
        }
        log.trace("Broadcasting satellite event key={}", record.key());
        broadcaster.broadcastSatellite(payload);
    }
}
