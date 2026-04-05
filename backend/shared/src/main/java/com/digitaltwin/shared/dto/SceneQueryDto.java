package com.digitaltwin.shared.dto;

import java.time.Instant;
import java.util.List;

public record SceneQueryDto(
        double minLat,
        double maxLat,
        double minLon,
        double maxLon,
        Instant from,
        Instant to,
        List<String> layers
) {}
