package com.digitaltwin.shared.dto;

import java.time.Instant;
import java.util.List;

public record SceneResultDto(
        List<AircraftPositionDto> flights,
        List<VesselPositionDto> ships,
        Instant queriedAt
) {}
