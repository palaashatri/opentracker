package com.digitaltwin.processor.consumer;

import com.digitaltwin.processor.processing.ShipProcessor;
import com.digitaltwin.shared.event.ShipRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShipRawConsumer {

    private final ShipProcessor processor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ships.raw", groupId = "stream-processor")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            ShipRawEvent event = objectMapper.readValue(record.value(), ShipRawEvent.class);
            processor.process(event);
        } catch (Exception e) {
            log.error("Failed to process ship event from topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }
}
