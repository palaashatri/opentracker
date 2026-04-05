package com.digitaltwin.shared.dto;

import java.time.Instant;

public record VesselPositionDto(
    String id,
    String vesselId,
    String mmsi,
    String name,
    Instant timestamp,
    double lat,
    double lon,
    Double speedKnots,
    Double courseDeg
) {}
