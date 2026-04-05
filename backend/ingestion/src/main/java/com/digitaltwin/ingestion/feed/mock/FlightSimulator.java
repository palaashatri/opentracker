package com.digitaltwin.ingestion.feed.mock;

import com.digitaltwin.shared.event.AircraftRawEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic flight simulator using a seeded {@link Random}.
 *
 * <p>Position update uses great-circle slerp steering toward waypoints with a
 * flat-earth dead-reckoning advance per tick:
 * <pre>
 *   lat += (speed_mps * cos(heading_rad) * dt_s) / 111_320
 *   lon += (speed_mps * sin(heading_rad) * dt_s) / (111_320 * cos(lat_rad))
 * </pre>
 *
 * <p>The same seed always produces the same initial positions, headings, and
 * waypoint sequences, making demos and CI runs reproducible.
 */
public class FlightSimulator {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double MIN_ALT_M             = 8_000.0;
    private static final double MAX_ALT_M             = 12_000.0;
    private static final double MIN_SPEED_MPS         = 200.0;
    private static final double MAX_SPEED_MPS         = 280.0;
    private static final double ALT_JITTER_M          = 50.0;
    private static final double SPEED_JITTER_MPS      = 5.0;
    private static final double WAYPOINT_ARRIVAL_DEG  = 1.0;
    private static final double MAX_TURN_RATE_DEG_S   = 5.0;

    private static final String[] AIRLINES  = {"AAL", "UAL", "DAL", "BAW", "AFR", "DLH", "SWA", "QFA", "SIA", "KLM"};
    private static final String[] MODELS    = {"B737", "B777", "A320", "A350", "B787", "A380", "B747"};
    private static final String[] COUNTRIES = {"US", "GB", "FR", "DE", "JP", "CN", "AU", "CA", "BR", "SG"};

    private final List<SimulatedAircraft> pool;
    private final Random                 rng;

    /**
     * @param poolSize number of aircraft to simulate
     * @param seed     deterministic seed — same seed = same positions every run
     */
    public FlightSimulator(int poolSize, long seed) {
        this.rng  = new Random(seed);
        this.pool = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(createAircraft(i));
        }
    }

    /**
     * Advances the simulation by {@code dtSeconds} and returns one event per aircraft.
     *
     * @param dtSeconds elapsed time in seconds since the last tick
     * @return snapshot of all aircraft positions
     */
    public List<AircraftRawEvent> tick(double dtSeconds) {
        Instant now    = Instant.now();
        List<AircraftRawEvent> events = new ArrayList<>(pool.size());
        for (SimulatedAircraft ac : pool) {
            advance(ac, dtSeconds);
            events.add(toEvent(ac, now));
        }
        return events;
    }

    // ─── Initialisation ───────────────────────────────────────────────────────

    private SimulatedAircraft createAircraft(int index) {
        SimulatedAircraft ac = new SimulatedAircraft();

        ac.icao24   = String.format("%06x", (index * 7919 + 0xA00000) & 0xFFFFFF);
        ac.airline  = AIRLINES[index % AIRLINES.length];
        ac.model    = MODELS[rng.nextInt(MODELS.length)];
        ac.country  = COUNTRIES[rng.nextInt(COUNTRIES.length)];
        ac.callsign = ac.airline + String.format("%04d", rng.nextInt(9000) + 1000);

        // Spread across known air corridors (avoid poles/oceans)
        ac.lat        = randomInRange(-60.0, 75.0);
        ac.lon        = randomInRange(-180.0, 180.0);
        ac.altitudeM  = randomInRange(MIN_ALT_M, MAX_ALT_M);
        ac.speedMps   = randomInRange(MIN_SPEED_MPS, MAX_SPEED_MPS);
        ac.headingDeg = rng.nextDouble() * 360.0;

        pickNewWaypoint(ac);
        return ac;
    }

    // ─── Simulation step ──────────────────────────────────────────────────────

    private void advance(SimulatedAircraft ac, double dtSeconds) {
        // Steer toward waypoint with capped turn rate
        double desiredHeading = bearingTo(ac.lat, ac.lon, ac.targetLat, ac.targetLon);
        double headingDelta   = normalizeAngle(desiredHeading - ac.headingDeg);
        double maxTurn        = MAX_TURN_RATE_DEG_S * dtSeconds;

        if (Math.abs(headingDelta) <= maxTurn) {
            ac.headingDeg = desiredHeading;
        } else {
            ac.headingDeg = normalizeHeading(ac.headingDeg + Math.signum(headingDelta) * maxTurn);
        }

        // Dead-reckoning position update
        double headingRad = Math.toRadians(ac.headingDeg);
        double cosLat     = Math.cos(Math.toRadians(ac.lat));
        if (Math.abs(cosLat) < 1e-9) cosLat = 1e-9;

        ac.lat += (ac.speedMps * Math.cos(headingRad) * dtSeconds) / METERS_PER_DEGREE_LAT;
        ac.lon += (ac.speedMps * Math.sin(headingRad) * dtSeconds) / (METERS_PER_DEGREE_LAT * cosLat);
        ac.lat  = Math.max(-85.0, Math.min(85.0, ac.lat));
        ac.lon  = wrapLon(ac.lon);

        // Jitter altitude and speed (uses rng for reproducibility)
        ac.altitudeM += (rng.nextDouble() - 0.5) * 2.0 * ALT_JITTER_M;
        ac.altitudeM  = Math.max(MIN_ALT_M - ALT_JITTER_M, Math.min(MAX_ALT_M + ALT_JITTER_M, ac.altitudeM));
        ac.speedMps  += (rng.nextDouble() - 0.5) * 2.0 * SPEED_JITTER_MPS;
        ac.speedMps   = Math.max(MIN_SPEED_MPS - SPEED_JITTER_MPS, Math.min(MAX_SPEED_MPS + SPEED_JITTER_MPS, ac.speedMps));

        // Waypoint arrival
        if (Math.hypot(ac.lat - ac.targetLat, ac.lon - ac.targetLon) < WAYPOINT_ARRIVAL_DEG) {
            pickNewWaypoint(ac);
        }
    }

    private void pickNewWaypoint(SimulatedAircraft ac) {
        ac.targetLat = randomInRange(-60.0, 75.0);
        ac.targetLon = randomInRange(-180.0, 180.0);
    }

    private AircraftRawEvent toEvent(SimulatedAircraft ac, Instant ts) {
        double verticalRate = (rng.nextDouble() - 0.5) * 4.0;
        return new AircraftRawEvent(
                ac.icao24, ac.callsign, ac.airline, ac.model, ac.country,
                "SIMULATED", ts,
                ac.lat, ac.lon, ac.altitudeM, ac.speedMps, ac.headingDeg, verticalRate
        );
    }

    // ─── Math helpers ─────────────────────────────────────────────────────────

    private static double bearingTo(double lat1, double lon1, double lat2, double lon2) {
        return normalizeHeading(Math.toDegrees(Math.atan2(lon2 - lon1, lat2 - lat1)));
    }

    private static double normalizeHeading(double deg) {
        deg = deg % 360.0;
        return deg < 0.0 ? deg + 360.0 : deg;
    }

    private static double normalizeAngle(double deg) {
        deg = deg % 360.0;
        if (deg >  180.0) deg -= 360.0;
        if (deg <= -180.0) deg += 360.0;
        return deg;
    }

    private static double wrapLon(double lon) {
        while (lon >= 180.0) lon -= 360.0;
        while (lon < -180.0) lon += 360.0;
        return lon;
    }

    private double randomInRange(double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }

    // ─── State ────────────────────────────────────────────────────────────────

    private static final class SimulatedAircraft {
        String icao24, callsign, airline, model, country;
        double lat, lon, altitudeM, speedMps, headingDeg;
        double targetLat, targetLon;
    }
}
