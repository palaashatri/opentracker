package com.digitaltwin.shared.dto;

import com.digitaltwin.shared.domain.DataSourceType;

import java.util.UUID;

public record AircraftDto(
    UUID id,
    String icao24,
    String callsign,
    String airline,
    String model,
    String country,
    DataSourceType source
) {}
