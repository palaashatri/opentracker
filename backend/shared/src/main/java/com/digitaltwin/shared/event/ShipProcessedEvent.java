package com.digitaltwin.shared.event;

import java.time.Instant;

public record ShipProcessedEvent(
        String vesselId,
        String mmsi,
        String imo,
        String name,
        String type,
        String flag,
        String source,
        Instant timestamp,
        double lat,
        double lon,
        Double speedKnots,
        Double courseDeg
) {}
