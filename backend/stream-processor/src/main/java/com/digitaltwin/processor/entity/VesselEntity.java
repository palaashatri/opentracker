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
@Table(name = "vessels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VesselEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "mmsi", nullable = false, length = 15)
    private String mmsi;

    @Column(name = "imo", length = 15)
    private String imo;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "type", length = 30)
    private String type;

    @Column(name = "flag", length = 10)
    private String flag;

    @Column(name = "source", nullable = false, length = 30)
    private String source;
}
