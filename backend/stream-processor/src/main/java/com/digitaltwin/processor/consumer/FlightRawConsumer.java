package com.digitaltwin.processor.consumer;

import com.digitaltwin.processor.processing.FlightProcessor;
import com.digitaltwin.shared.event.AircraftRawEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlightRawConsumer {

    private final FlightProcessor processor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "flights.raw", groupId = "stream-processor")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            AircraftRawEvent event = objectMapper.readValue(record.value(), AircraftRawEvent.class);
            processor.process(event);
        } catch (Exception e) {
            log.error("Failed to process flight event from topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset(), e);
        }
    }
}
