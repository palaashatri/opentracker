package com.digitaltwin.shared.kafka;

public interface KafkaTopics {
    String FLIGHTS_RAW       = "flights.raw";
    String SHIPS_RAW         = "ships.raw";
    String SATELLITES_RAW    = "satellites.raw";
    String FLIGHTS_PROCESSED = "flights.processed";
    String SHIPS_PROCESSED   = "ships.processed";
    String SATELLITES_PROCESSED = "satellites.processed";
}
