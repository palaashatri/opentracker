package com.digitaltwin.processor.repository;

import com.digitaltwin.processor.entity.AircraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AircraftRepository extends JpaRepository<AircraftEntity, UUID> {

    Optional<AircraftEntity> findByIcao24(String icao24);
}
