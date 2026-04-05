package com.digitaltwin.ingestion.feed.mock;

import com.digitaltwin.ingestion.config.MockProperties;
import com.digitaltwin.ingestion.feed.FlightFeedProvider;
import com.digitaltwin.shared.event.AircraftRawEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock implementation of {@link FlightFeedProvider} backed by a deterministic
 * {@link FlightSimulator} seeded from {@link MockProperties}.
 *
 * <p>Active when {@code ingestion.feed.flight=mock} (the default).
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.flight", havingValue = "mock", matchIfMissing = true)
public class MockFlightFeedProvider implements FlightFeedProvider {

    private final FlightSimulator simulator;

    public MockFlightFeedProvider(MockProperties mock) {
        this.simulator = new FlightSimulator(mock.getFlightsCount(), mock.getSeed());
    }

    @Override
    public List<AircraftRawEvent> fetch() {
        return simulator.tick(1.0);
    }
}
