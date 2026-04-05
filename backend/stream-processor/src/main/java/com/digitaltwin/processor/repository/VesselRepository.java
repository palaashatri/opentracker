package com.digitaltwin.processor.repository;

import com.digitaltwin.processor.entity.VesselEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VesselRepository extends JpaRepository<VesselEntity, UUID> {

    Optional<VesselEntity> findByMmsi(String mmsi);
}
