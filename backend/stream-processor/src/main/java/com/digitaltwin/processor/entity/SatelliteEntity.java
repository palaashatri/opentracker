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
@Table(name = "satellites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "norad_id", nullable = false, length = 10)
    private String noradId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "source", length = 30)
    private String source;
}
