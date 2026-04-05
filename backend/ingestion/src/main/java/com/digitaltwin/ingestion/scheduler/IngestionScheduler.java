package com.digitaltwin.ingestion.scheduler;

import com.digitaltwin.ingestion.config.IngestionProperties;
import com.digitaltwin.ingestion.feed.FlightFeedProvider;
import com.digitaltwin.ingestion.feed.ShipFeedProvider;
import com.digitaltwin.ingestion.publisher.FlightEventPublisher;
import com.digitaltwin.ingestion.publisher.ShipEventPublisher;
import com.digitaltwin.shared.event.AircraftRawEvent;
import com.digitaltwin.shared.event.ShipRawEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drives the ingestion loop on a fixed-delay schedule.
 *
 * <p>Two independent scheduled methods handle flights and ships respectively,
 * each running at the interval configured via {@code ingestion.simulation.interval-ms}.
 * Using {@code fixedDelayString} means the next execution begins only after
 * the previous one completes, which prevents pile-up if Kafka is slow.
 */
@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final FlightFeedProvider flightProvider;
    private final ShipFeedProvider shipProvider;
    private final FlightEventPublisher flightPublisher;
    private final ShipEventPublisher shipPublisher;
    private final IngestionProperties properties;

    public IngestionScheduler(
            FlightFeedProvider flightProvider,
            ShipFeedProvider shipProvider,
            FlightEventPublisher flightPublisher,
            ShipEventPublisher shipPublisher,
            IngestionProperties properties) {
        this.flightProvider = flightProvider;
        this.shipProvider = shipProvider;
        this.flightPublisher = flightPublisher;
        this.shipPublisher = shipPublisher;
        this.properties = properties;
    }

    /**
     * Fetches the latest aircraft positions and publishes each to Kafka.
     */
    @Scheduled(fixedDelayString = "${ingestion.simulation.interval-ms:1000}")
    public void ingestFlights() {
        List<AircraftRawEvent> events = flightProvider.fetch();
        if (log.isDebugEnabled()) {
            log.debug("Ingesting {} flight events", events.size());
        }
        events.forEach(flightPublisher::publish);
    }

    /**
     * Fetches the latest vessel positions and publishes each to Kafka.
     */
    @Scheduled(fixedDelayString = "${ingestion.simulation.interval-ms:1000}")
    public void ingestShips() {
        List<ShipRawEvent> events = shipProvider.fetch();
        if (log.isDebugEnabled()) {
            log.debug("Ingesting {} ship events", events.size());
        }
        events.forEach(shipPublisher::publish);
    }
}
