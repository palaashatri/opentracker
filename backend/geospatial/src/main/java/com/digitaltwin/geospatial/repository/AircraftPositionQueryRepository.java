package com.digitaltwin.geospatial.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AircraftPositionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns recent positions within the given bounding box.
     * Columns: id, aircraft_id, icao24, callsign, recorded_at,
     *          lat, lon, altitude_m, ground_speed_mps, heading_deg, vertical_rate_mps
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findCurrentByBbox(double minLat, double maxLat, double minLon, double maxLon) {
        String sql = """
                SELECT ap.id, ap.aircraft_id, a.icao24, a.callsign, ap.recorded_at,
                       ap.lat, ap.lon, ap.altitude_m, ap.ground_speed_mps,
                       ap.heading_deg, ap.vertical_rate_mps
                FROM aircraft_positions ap
                JOIN aircraft a ON a.id = ap.aircraft_id
                WHERE ap.lat BETWEEN :minLat AND :maxLat
                  AND ap.lon BETWEEN :minLon AND :maxLon
                  AND ap.recorded_at >= :since
                ORDER BY ap.recorded_at DESC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("minLat", minLat)
                .setParameter("maxLat", maxLat)
                .setParameter("minLon", minLon)
                .setParameter("maxLon", maxLon)
                .setParameter("since", Timestamp.from(Instant.now().minusSeconds(120)))
                .getResultList();
    }

    /**
     * Returns all positions for a specific aircraft within the given time range.
     * Columns: id, aircraft_id, icao24, callsign, recorded_at,
     *          lat, lon, altitude_m, ground_speed_mps, heading_deg, vertical_rate_mps
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findTrack(UUID aircraftId, Instant from, Instant to) {
        String sql = """
                SELECT ap.id, ap.aircraft_id, a.icao24, a.callsign, ap.recorded_at,
                       ap.lat, ap.lon, ap.altitude_m, ap.ground_speed_mps,
                       ap.heading_deg, ap.vertical_rate_mps
                FROM aircraft_positions ap
                JOIN aircraft a ON a.id = ap.aircraft_id
                WHERE ap.aircraft_id = :aircraftId
                  AND ap.recorded_at BETWEEN :from AND :to
                ORDER BY ap.recorded_at ASC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("aircraftId", aircraftId)
                .setParameter("from", Timestamp.from(from))
                .setParameter("to", Timestamp.from(to))
                .getResultList();
    }
}
