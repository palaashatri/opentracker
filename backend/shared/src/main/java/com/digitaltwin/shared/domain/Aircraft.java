package com.digitaltwin.shared.domain;

import java.util.UUID;

public record Aircraft(
    UUID id,
    String icao24,
    String callsign,
    String airline,
    String model,
    String country,
    DataSourceType source
) {}
