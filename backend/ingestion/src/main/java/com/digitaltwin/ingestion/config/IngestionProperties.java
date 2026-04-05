package com.digitaltwin.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ingestion")
@Component
public class IngestionProperties {

    private Feed feed = new Feed();
    private Simulation simulation = new Simulation();

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public static class Feed {

        private String flight = "mock";
        private String ship = "mock";

        public String getFlight() {
            return flight;
        }

        public void setFlight(String flight) {
            this.flight = flight;
        }

        public String getShip() {
            return ship;
        }

        public void setShip(String ship) {
            this.ship = ship;
        }
    }

    public static class Simulation {

        private int aircraftCount = 500;
        private int vesselCount = 200;
        private int intervalMs = 1000;
        private int batchSize = 50;

        public int getAircraftCount() {
            return aircraftCount;
        }

        public void setAircraftCount(int aircraftCount) {
            this.aircraftCount = aircraftCount;
        }

        public int getVesselCount() {
            return vesselCount;
        }

        public void setVesselCount(int vesselCount) {
            this.vesselCount = vesselCount;
        }

        public int getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(int intervalMs) {
            this.intervalMs = intervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
