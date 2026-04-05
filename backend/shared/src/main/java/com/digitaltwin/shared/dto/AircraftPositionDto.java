package com.digitaltwin.shared.dto;

import java.time.Instant;

public record AircraftPositionDto(
    String id,
    String aircraftId,
    String icao24,
    String callsign,
    Instant timestamp,
    double lat,
    double lon,
    Double altitudeMeters,
    Double groundSpeedMps,
    Double headingDeg,
    Double verticalRateMps
) {}
