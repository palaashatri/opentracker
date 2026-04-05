package com.digitaltwin.shared.domain;

import java.util.UUID;

public record Vessel(
    UUID id,
    String mmsi,
    String imo,
    String name,
    String type,
    String flag,
    DataSourceType source
) {}
