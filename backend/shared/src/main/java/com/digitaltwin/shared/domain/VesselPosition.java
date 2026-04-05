package com.digitaltwin.shared.domain;

import java.time.Instant;
import java.util.UUID;

public record VesselPosition(
    UUID id,
    UUID vesselId,
    Instant timestamp,
    double lat,
    double lon,
    Double speedKnots,
    Double courseDeg
) {}
