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
@Table(name = "aircraft_positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AircraftPositionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "aircraft_id", columnDefinition = "uuid", nullable = false)
    private UUID aircraftId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Column(name = "altitude_m")
    private Double altitudeMeters;

    @Column(name = "ground_speed_mps")
    private Double groundSpeedMps;

    @Column(name = "heading_deg")
    private Double headingDeg;

    @Column(name = "vertical_rate_mps")
    private Double verticalRateMps;
}
