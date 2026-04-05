package com.digitaltwin.ingestion.feed;

import com.digitaltwin.shared.event.AircraftRawEvent;

import java.util.List;

public interface FlightFeedProvider {

    /**
     * Fetches the latest batch of aircraft position events.
     *
     * @return list of raw aircraft events representing current positions
     */
    List<AircraftRawEvent> fetch();
}
