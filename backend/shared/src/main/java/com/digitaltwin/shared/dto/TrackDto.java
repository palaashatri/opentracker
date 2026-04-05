package com.digitaltwin.shared.dto;

import java.util.List;

public record TrackDto(
        String entityId,
        String entityType,
        List<TrackPointDto> points
) {}
