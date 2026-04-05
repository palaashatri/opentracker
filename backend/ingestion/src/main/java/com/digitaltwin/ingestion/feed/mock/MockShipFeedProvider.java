package com.digitaltwin.ingestion.feed.mock;

import com.digitaltwin.ingestion.config.MockProperties;
import com.digitaltwin.ingestion.feed.ShipFeedProvider;
import com.digitaltwin.shared.event.ShipRawEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock implementation of {@link ShipFeedProvider} backed by a deterministic
 * {@link ShipSimulator} seeded from {@link MockProperties}.
 *
 * <p>Active when {@code ingestion.feed.ship=mock} (the default).
 */
@Component
@ConditionalOnProperty(name = "ingestion.feed.ship", havingValue = "mock", matchIfMissing = true)
public class MockShipFeedProvider implements ShipFeedProvider {

    private final ShipSimulator simulator;

    public MockShipFeedProvider(MockProperties mock) {
        this.simulator = new ShipSimulator(mock.getShipsCount(), mock.getSeed());
    }

    @Override
    public List<ShipRawEvent> fetch() {
        return simulator.tick(1.0);
    }
}
