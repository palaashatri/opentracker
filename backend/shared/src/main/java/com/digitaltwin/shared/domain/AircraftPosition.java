package com.digitaltwin.shared.domain;

import java.time.Instant;
import java.util.UUID;

public record AircraftPosition(
    UUID id,
    UUID aircraftId,
    Instant timestamp,
    double lat,
    double lon,
    Double altitudeMeters,
    Double groundSpeedMps,
    Double headingDeg,
    Double verticalRateMps
) {}
