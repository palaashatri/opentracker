package com.digitaltwin.processor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "aircraft")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AircraftEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "icao24", nullable = false, length = 10)
    private String icao24;

    @Column(name = "callsign", length = 20)
    private String callsign;

    @Column(name = "airline", length = 50)
    private String airline;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "source", nullable = false, length = 30)
    private String source;
}
