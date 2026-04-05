package com.digitaltwin.shared.event;

import java.time.Instant;

public record SatelliteRawEvent(
    String noradId,
    String name,
    String source,
    Instant timestamp,
    double lat,
    double lon,
    double altitudeKm,
    double velocityKmS
) {}
