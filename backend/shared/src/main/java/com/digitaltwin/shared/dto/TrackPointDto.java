package com.digitaltwin.shared.dto;

import java.time.Instant;

public record TrackPointDto(
        Instant timestamp,
        double lat,
        double lon,
        Double altitudeMeters,
        Double headingDeg
) {}
