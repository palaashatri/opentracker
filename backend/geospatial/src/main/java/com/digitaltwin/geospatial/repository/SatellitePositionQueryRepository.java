package com.digitaltwin.geospatial.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class SatellitePositionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns recent satellite positions from the last 120 seconds.
     * Columns: id(0), satellite_id(1), norad_id(2), name(3), recorded_at(4),
     *          lat(5), lon(6), altitude_km(7), velocity_km_s(8)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAllCurrent() {
        String sql = """
                SELECT sp.id, sp.satellite_id, s.norad_id, s.name, sp.recorded_at,
                       sp.lat, sp.lon, sp.altitude_km, sp.velocity_km_s
                FROM satellite_positions sp
                JOIN satellites s ON s.id = sp.satellite_id
                WHERE sp.recorded_at >= :since
                ORDER BY sp.recorded_at DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("since", Timestamp.from(Instant.now().minusSeconds(120)))
                .getResultList();
    }
}
