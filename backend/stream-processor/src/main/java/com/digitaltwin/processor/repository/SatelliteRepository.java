package com.digitaltwin.processor.repository;

import com.digitaltwin.processor.entity.SatelliteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SatelliteRepository extends JpaRepository<SatelliteEntity, UUID> {

    Optional<SatelliteEntity> findByNoradId(String noradId);
}
