package com.digitaltwin.processor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "satellite_positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatellitePositionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "satellite_id", columnDefinition = "uuid", nullable = false)
    private UUID satelliteId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Column(name = "altitude_km", nullable = false)
    private double altitudeKm;

    @Column(name = "velocity_km_s")
    private Double velocityKmS;
}
