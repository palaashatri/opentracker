package com.digitaltwin.shared.event;

import java.time.Instant;

public record AircraftProcessedEvent(
        String aircraftId,
        String icao24,
        String callsign,
        String airline,
        String model,
        String country,
        String source,
        Instant timestamp,
        double lat,
        double lon,
        Double altitudeMeters,
        Double groundSpeedMps,
        Double headingDeg,
        Double verticalRateMps
) {}
