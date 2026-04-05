package com.digitaltwin.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for deterministic mock data generation.
 *
 * <p>All mock simulators share the same seed so that a given seed value
 * always produces the same initial positions and trajectories.
 * This makes demos and CI results reproducible.
 */
@ConfigurationProperties(prefix = "mock")
@Component
public class MockProperties {

    /** Random seed for all mock simulators. */
    private long seed = 42;

    /** Number of mock aircraft to simulate. */
    private int flightsCount = 500;

    /** Number of mock vessels to simulate. */
    private int shipsCount = 200;

    /** Number of mock satellites to simulate. */
    private int satellitesCount = 100;

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getFlightsCount() {
        return flightsCount;
    }

    public void setFlightsCount(int flightsCount) {
        this.flightsCount = flightsCount;
    }

    public int getShipsCount() {
        return shipsCount;
    }

    public void setShipsCount(int shipsCount) {
        this.shipsCount = shipsCount;
    }

    public int getSatellitesCount() {
        return satellitesCount;
    }

    public void setSatellitesCount(int satellitesCount) {
        this.satellitesCount = satellitesCount;
    }
}
