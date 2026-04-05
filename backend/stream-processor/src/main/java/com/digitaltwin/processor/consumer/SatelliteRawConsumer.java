package com.digitaltwin.processor.consumer;

import com.digitaltwin.processor.processing.SatelliteProcessor;
import com.digitaltwin.shared.event.SatelliteRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SatelliteRawConsumer {

    private final SatelliteProcessor processor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "satellites.raw", groupId = "stream-processor-satellites")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            SatelliteRawEvent event = objectMapper.readValue(record.value(), SatelliteRawEvent.class);
            processor.process(event);
        } catch (Exception e) {
            log.error("Failed to process satellite event from topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }
}
