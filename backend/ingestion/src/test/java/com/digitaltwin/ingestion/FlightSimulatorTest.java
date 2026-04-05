package com.digitaltwin.ingestion;

import com.digitaltwin.ingestion.feed.mock.FlightSimulator;
import com.digitaltwin.shared.event.AircraftRawEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightSimulatorTest {

    @Test
    void tickReturnsSameCountAsPoolSize() {
        FlightSimulator sim = new FlightSimulator(10);
        List<AircraftRawEvent> events = sim.tick(1.0);
        assertEquals(10, events.size());
    }

    @Test
    void aircraftPositionsAreWithinBounds() {
        FlightSimulator sim = new FlightSimulator(50);
        // Run several ticks to ensure movement stays in bounds
        for (int i = 0; i < 100; i++) {
            List<AircraftRawEvent> events = sim.tick(1.0);
            for (AircraftRawEvent e : events) {
                assertTrue(e.lat() >= -85.0 && e.lat() <= 85.0,
                        "Latitude out of range: " + e.lat());
                assertTrue(e.lon() >= -180.0 && e.lon() < 180.0,
                        "Longitude out of range: " + e.lon());
            }
        }
    }

    @Test
    void aircraftAltitudeIsReasonable() {
        FlightSimulator sim = new FlightSimulator(20);
        List<AircraftRawEvent> events = sim.tick(1.0);
        for (AircraftRawEvent e : events) {
            assertNotNull(e.altitudeMeters());
            assertTrue(e.altitudeMeters() >= 7000.0 && e.altitudeMeters() <= 13000.0,
                    "Altitude out of cruise range: " + e.altitudeMeters());
        }
    }

    @Test
    void aircraftMovesBetweenTicks() {
        FlightSimulator sim = new FlightSimulator(5);
        List<AircraftRawEvent> first = sim.tick(1.0);
        List<AircraftRawEvent> second = sim.tick(1.0);

        // At least one aircraft should have moved
        boolean anyMoved = false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).lat() != second.get(i).lat()
                    || first.get(i).lon() != second.get(i).lon()) {
                anyMoved = true;
                break;
            }
        }
        assertTrue(anyMoved, "No aircraft moved between ticks");
    }

    @Test
    void aircraftHaveUniqueIcao24() {
        FlightSimulator sim = new FlightSimulator(100);
        List<AircraftRawEvent> events = sim.tick(1.0);
        long distinctCount = events.stream().map(AircraftRawEvent::icao24).distinct().count();
        assertEquals(100, distinctCount, "Duplicate ICAO24 codes found");
    }

    @Test
    void sourceIsSimulated() {
        FlightSimulator sim = new FlightSimulator(5);
        List<AircraftRawEvent> events = sim.tick(1.0);
        for (AircraftRawEvent e : events) {
            assertEquals("SIMULATED", e.source());
        }
    }
}
