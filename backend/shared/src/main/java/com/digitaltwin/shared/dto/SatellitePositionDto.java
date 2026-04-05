package com.digitaltwin.shared.dto;

import java.time.Instant;

public record SatellitePositionDto(
    String id,
    String noradId,
    String name,
    Instant timestamp,
    double lat,
    double lon,
    double altitudeKm,
    double velocityKmS
) {}
