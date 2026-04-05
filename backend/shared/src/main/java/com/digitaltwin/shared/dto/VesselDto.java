package com.digitaltwin.shared.dto;

import com.digitaltwin.shared.domain.DataSourceType;

import java.util.UUID;

public record VesselDto(
    UUID id,
    String mmsi,
    String imo,
    String name,
    String type,
    String flag,
    DataSourceType source
) {}
