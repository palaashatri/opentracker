package com.digitaltwin.ingestion;

import com.digitaltwin.ingestion.feed.mock.ShipSimulator;
import com.digitaltwin.shared.event.ShipRawEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipSimulatorTest {

    @Test
    void tickReturnsSameCountAsPoolSize() {
        ShipSimulator sim = new ShipSimulator(10);
        List<ShipRawEvent> events = sim.tick(1.0);
        assertEquals(10, events.size());
    }

    @Test
    void vesselPositionsAreWithinBounds() {
        ShipSimulator sim = new ShipSimulator(30);
        for (int i = 0; i < 200; i++) {
            List<ShipRawEvent> events = sim.tick(1.0);
            for (ShipRawEvent e : events) {
                assertTrue(e.lat() >= -85.0 && e.lat() <= 85.0,
                        "Latitude out of range: " + e.lat());
                assertTrue(e.lon() >= -180.0 && e.lon() < 180.0,
                        "Longitude out of range: " + e.lon());
            }
        }
    }

    @Test
    void vesselSpeedIsInKnots() {
        ShipSimulator sim = new ShipSimulator(20);
        List<ShipRawEvent> events = sim.tick(1.0);
        for (ShipRawEvent e : events) {
            assertNotNull(e.speedKnots());
            // 3 m/s = ~5.8 kn, 8 m/s = ~15.5 kn, with jitter
            assertTrue(e.speedKnots() >= 5.0 && e.speedKnots() <= 17.0,
                    "Speed out of expected range: " + e.speedKnots());
        }
    }

    @Test
    void vesselsMovesBetweenTicks() {
        ShipSimulator sim = new ShipSimulator(5);
        List<ShipRawEvent> first = sim.tick(1.0);
        List<ShipRawEvent> second = sim.tick(1.0);

        boolean anyMoved = false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).lat() != second.get(i).lat()
                    || first.get(i).lon() != second.get(i).lon()) {
                anyMoved = true;
                break;
            }
        }
        assertTrue(anyMoved, "No vessel moved between ticks");
    }

    @Test
    void mmsiIsNineDigits() {
        ShipSimulator sim = new ShipSimulator(20);
        List<ShipRawEvent> events = sim.tick(1.0);
        for (ShipRawEvent e : events) {
            assertNotNull(e.mmsi());
            assertEquals(9, e.mmsi().length(), "MMSI is not 9 digits: " + e.mmsi());
            assertTrue(e.mmsi().matches("\\d{9}"), "MMSI contains non-digits: " + e.mmsi());
        }
    }

    @Test
    void sourceIsSimulated() {
        ShipSimulator sim = new ShipSimulator(5);
        List<ShipRawEvent> events = sim.tick(1.0);
        for (ShipRawEvent e : events) {
            assertEquals("SIMULATED", e.source());
        }
    }
}
