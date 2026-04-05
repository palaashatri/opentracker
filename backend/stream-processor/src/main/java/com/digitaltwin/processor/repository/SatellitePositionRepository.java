package com.digitaltwin.processor.repository;

import com.digitaltwin.processor.entity.SatellitePositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SatellitePositionRepository extends JpaRepository<SatellitePositionEntity, UUID> {
}
