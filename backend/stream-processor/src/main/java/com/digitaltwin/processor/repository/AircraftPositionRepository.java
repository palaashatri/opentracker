package com.digitaltwin.processor.repository;

import com.digitaltwin.processor.entity.AircraftPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AircraftPositionRepository extends JpaRepository<AircraftPositionEntity, UUID> {
}
