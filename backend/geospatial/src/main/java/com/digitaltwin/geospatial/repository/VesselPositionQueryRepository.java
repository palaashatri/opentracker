package com.digitaltwin.geospatial.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class VesselPositionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns recent positions within the given bounding box.
     * Columns: id, vessel_id, mmsi, name, recorded_at,
     *          lat, lon, speed_knots, course_deg
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findCurrentByBbox(double minLat, double maxLat, double minLon, double maxLon) {
        String sql = """
                SELECT vp.id, vp.vessel_id, v.mmsi, v.name, vp.recorded_at,
                       vp.lat, vp.lon, vp.speed_knots, vp.course_deg
                FROM vessel_positions vp
                JOIN vessels v ON v.id = vp.vessel_id
                WHERE vp.lat BETWEEN :minLat AND :maxLat
                  AND vp.lon BETWEEN :minLon AND :maxLon
                  AND vp.recorded_at >= :since
                ORDER BY vp.recorded_at DESC
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
     * Returns all positions for a specific vessel within the given time range.
     * Columns: id, vessel_id, mmsi, name, recorded_at,
     *          lat, lon, speed_knots, course_deg
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findTrack(UUID vesselId, Instant from, Instant to) {
        String sql = """
                SELECT vp.id, vp.vessel_id, v.mmsi, v.name, vp.recorded_at,
                       vp.lat, vp.lon, vp.speed_knots, vp.course_deg
                FROM vessel_positions vp
                JOIN vessels v ON v.id = vp.vessel_id
                WHERE vp.vessel_id = :vesselId
                  AND vp.recorded_at BETWEEN :from AND :to
                ORDER BY vp.recorded_at ASC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("vesselId", vesselId)
                .setParameter("from", Timestamp.from(from))
                .setParameter("to", Timestamp.from(to))
                .getResultList();
    }
}
