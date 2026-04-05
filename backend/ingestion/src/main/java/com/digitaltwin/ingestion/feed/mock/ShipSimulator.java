package com.digitaltwin.ingestion.feed.mock;

import com.digitaltwin.shared.event.ShipRawEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic ship simulator using a seeded {@link Random}.
 *
 * <p>Ships move at 3–8 m/s (≈ 6–15 knots) along major sea lanes.
 * Starting positions are seeded into realistic ocean regions.
 * The same seed always produces the same initial positions and trajectories.
 */
public class ShipSimulator {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double MIN_SPEED_MPS          = 3.0;   // ≈ 5.8 kn
    private static final double MAX_SPEED_MPS          = 8.0;   // ≈ 15.5 kn
    private static final double SPEED_JITTER_MPS       = 0.3;
    private static final double WAYPOINT_ARRIVAL_DEG   = 0.5;
    private static final double MAX_TURN_RATE_DEG_S    = 2.0;

    private static final String[] TYPES = {"CARGO", "TANKER", "PASSENGER", "FISHING", "MILITARY"};
    private static final String[] FLAGS = {"US", "PA", "LR", "MH", "BS", "CN", "NO"};

    /**
     * Ocean/sea-lane seed regions: {minLat, maxLat, minLon, maxLon}.
     * Covers major shipping corridors from MOCK_DATA.md.
     */
    private static final double[][] SEA_LANES = {
            // North Atlantic (transatlantic shipping)
            {20.0,  60.0, -70.0, -10.0},
            // South Atlantic
            {-50.0, 10.0, -55.0,  20.0},
            // North Pacific
            {10.0,  55.0, 120.0,  240.0},  // 240 = -120 wrapped
            // South Pacific
            {-50.0, 10.0, 140.0,  290.0},  // 290 = -70 wrapped
            // Indian Ocean
            {-40.0, 25.0,  40.0,  100.0},
            // Mediterranean
            {30.0,  46.0,  -5.0,   37.0},
            // Suez corridor
            {12.0,  32.0,  32.0,   45.0},
            // Malacca Strait / South China Sea
            {1.0,   20.0,  98.0,  120.0},
            // Arctic routes
            {60.0,  75.0, -30.0,   50.0},
    };

    private final List<SimulatedVessel> pool;
    private final Random                rng;

    /**
     * @param poolSize number of vessels to simulate
     * @param seed     deterministic seed — same seed = same positions every run
     */
    public ShipSimulator(int poolSize, long seed) {
        this.rng  = new Random(seed);
        this.pool = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(createVessel(i));
        }
    }

    /**
     * Advances the simulation by {@code dtSeconds} and returns one event per vessel.
     *
     * @param dtSeconds elapsed time in seconds
     * @return list of raw ship events
     */
    public List<ShipRawEvent> tick(double dtSeconds) {
        Instant now    = Instant.now();
        List<ShipRawEvent> events = new ArrayList<>(pool.size());
        for (SimulatedVessel v : pool) {
            advance(v, dtSeconds);
            events.add(toEvent(v, now));
        }
        return events;
    }

    // ─── Initialisation ───────────────────────────────────────────────────────

    private SimulatedVessel createVessel(int index) {
        SimulatedVessel v = new SimulatedVessel();

        int[] mmsiPrefixes = {338, 352, 636, 538, 311, 412, 257};
        int prefix = mmsiPrefixes[index % mmsiPrefixes.length];
        int suffix = rng.nextInt(1_000_000) + 1;
        v.mmsi = String.format("%03d%06d", prefix, suffix);
        v.imo  = "IMO" + String.format("%07d", rng.nextInt(9_000_000) + 1_000_000);
        v.type = TYPES[rng.nextInt(TYPES.length)];
        v.flag = FLAGS[index % FLAGS.length];
        v.name = generateName(index);

        double[] lane = SEA_LANES[rng.nextInt(SEA_LANES.length)];
        v.lat       = randomInRange(lane[0], lane[1]);
        v.lon       = wrapLon(randomInRange(lane[2], lane[3]));
        v.speedMps  = randomInRange(MIN_SPEED_MPS, MAX_SPEED_MPS);
        v.headingDeg = rng.nextDouble() * 360.0;

        pickNewWaypoint(v);
        return v;
    }

    // ─── Simulation step ──────────────────────────────────────────────────────

    private void advance(SimulatedVessel v, double dtSeconds) {
        double desired     = bearingTo(v.lat, v.lon, v.targetLat, v.targetLon);
        double delta       = normalizeAngle(desired - v.headingDeg);
        double maxTurn     = MAX_TURN_RATE_DEG_S * dtSeconds;

        if (Math.abs(delta) <= maxTurn) {
            v.headingDeg = desired;
        } else {
            v.headingDeg = normalizeHeading(v.headingDeg + Math.signum(delta) * maxTurn);
        }

        double headingRad = Math.toRadians(v.headingDeg);
        double cosLat     = Math.cos(Math.toRadians(v.lat));
        if (Math.abs(cosLat) < 1e-9) cosLat = 1e-9;

        v.lat += (v.speedMps * Math.cos(headingRad) * dtSeconds) / METERS_PER_DEGREE_LAT;
        v.lon += (v.speedMps * Math.sin(headingRad) * dtSeconds) / (METERS_PER_DEGREE_LAT * cosLat);
        v.lat  = Math.max(-85.0, Math.min(85.0, v.lat));
        v.lon  = wrapLon(v.lon);

        v.speedMps += (rng.nextDouble() - 0.5) * 2.0 * SPEED_JITTER_MPS;
        v.speedMps  = Math.max(MIN_SPEED_MPS - SPEED_JITTER_MPS, Math.min(MAX_SPEED_MPS + SPEED_JITTER_MPS, v.speedMps));

        if (Math.hypot(v.lat - v.targetLat, v.lon - v.targetLon) < WAYPOINT_ARRIVAL_DEG) {
            pickNewWaypoint(v);
        }
    }

    private void pickNewWaypoint(SimulatedVessel v) {
        double[] lane    = SEA_LANES[rng.nextInt(SEA_LANES.length)];
        v.targetLat      = randomInRange(lane[0], lane[1]);
        v.targetLon      = wrapLon(randomInRange(lane[2], lane[3]));
    }

    private ShipRawEvent toEvent(SimulatedVessel v, Instant ts) {
        double speedKnots = v.speedMps * 1.94384;
        return new ShipRawEvent(
                v.mmsi, v.imo, v.name, v.type, v.flag,
                "SIMULATED", ts,
                v.lat, v.lon, speedKnots, v.headingDeg
        );
    }

    private static String generateName(int index) {
        String[] pfx = {"ATLANTIC", "PACIFIC", "OCEAN", "SEA", "NORTH", "SOUTH", "GLOBAL", "MARITIME", "HORIZON", "VOYAGER"};
        String[] sfx = {"STAR", "QUEEN", "KING", "PRIDE", "SPIRIT", "PIONEER", "EXPLORER", "TRADER", "EAGLE", "DAWN"};
        return pfx[index % pfx.length] + " " + sfx[(index / pfx.length) % sfx.length];
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

    private static final class SimulatedVessel {
        String mmsi, imo, name, type, flag;
        double lat, lon, speedMps, headingDeg;
        double targetLat, targetLon;
    }
}
