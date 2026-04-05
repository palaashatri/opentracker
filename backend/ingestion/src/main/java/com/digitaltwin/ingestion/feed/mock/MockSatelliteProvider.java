package com.digitaltwin.ingestion.feed.mock;

import com.digitaltwin.ingestion.config.MockProperties;
import com.digitaltwin.ingestion.publisher.SatelliteEventPublisher;
import com.digitaltwin.shared.event.SatelliteRawEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic mock satellite provider.
 *
 * <p>Simulates N satellites in circular orbits using two-body mechanics
 * as specified in MOCK_DATA.md:
 * <pre>
 *   ω = sqrt(μ / r³)     — mean motion for circular orbit at radius r
 *   θ(t) = θ₀ + ω·t     — true anomaly advances linearly
 *   x = r·cos(θ)        — position in orbital plane
 *   y = r·sin(θ)
 *   then rotate by inclination i and RAAN Ω to ECEF
 *   convert ECEF → geodetic
 * </pre>
 *
 * <p>Orbital velocity: v = sqrt(μ/r) (circular orbit vis-viva).
 *
 * <p>Active when {@code ingestion.satellite.enabled=false} (or real TLE feed is disabled)
 * and the {@code mock} Spring profile is active, OR when
 * {@code ingestion.feed.satellite=mock}.
 */
@Component
@ConditionalOnProperty(name = "ingestion.satellite.enabled", havingValue = "false")
@Slf4j
public class MockSatelliteProvider {

    // ─── Physical constants ───────────────────────────────────────────────────

    /** Earth gravitational parameter μ (km³ s⁻²). */
    private static final double MU = 398_600.4418;
    /** WGS-84 semi-major axis (km). */
    private static final double RE = 6_378.137;
    /** WGS-84 first eccentricity squared. */
    private static final double E2 = 0.006_694_379_990_14;

    // ─── Altitude bands (km above surface) matching MOCK_DATA.md ────────────

    /** LEO: 200–2000 km */
    private static final double LEO_MIN = 200.0;
    private static final double LEO_MAX = 2_000.0;
    /** MEO: 2000–35000 km */
    private static final double MEO_MIN = 2_000.0;
    private static final double MEO_MAX = 35_000.0;
    /** GEO: ~35786 km */
    private static final double GEO_ALT = 35_786.0;

    private final SatelliteEventPublisher publisher;
    private final List<MockSatellite>     satellites = new ArrayList<>();
    private long                          startMs;

    public MockSatelliteProvider(MockProperties mock, SatelliteEventPublisher publisher) {
        this.publisher = publisher;
        Random rng = new Random(mock.getSeed() + 1_000_000L); // distinct seed from flights/ships

        int n = mock.getSatellitesCount();
        for (int i = 0; i < n; i++) {
            satellites.add(createSatellite(i, n, rng));
        }
        log.info("MockSatelliteProvider: initialized {} mock satellites", n);
    }

    @PostConstruct
    public void init() {
        this.startMs = System.currentTimeMillis();
    }

    /**
     * Propagates all mock satellites to the current time and publishes events.
     * Runs every second.
     */
    @Scheduled(fixedDelay = 1_000)
    public void propagate() {
        long nowMs = System.currentTimeMillis();
        double tSeconds = (nowMs - startMs) / 1_000.0;
        Instant now = Instant.ofEpochMilli(nowMs);

        for (MockSatellite sat : satellites) {
            double[] geo = sat.positionAt(tSeconds);
            if (geo == null) continue;

            SatelliteRawEvent event = new SatelliteRawEvent(
                    sat.noradId,
                    sat.name,
                    "MOCK",
                    now,
                    geo[0],   // lat deg
                    geo[1],   // lon deg
                    geo[2],   // alt km
                    sat.velocityKmS
            );
            publisher.publish(event);
        }
    }

    // ─── Satellite factory ────────────────────────────────────────────────────

    private static MockSatellite createSatellite(int index, int total, Random rng) {
        // Assign to altitude band: ~60% LEO, ~30% MEO, ~10% GEO
        double altKm;
        int bandRoll = index % 10;
        if (bandRoll < 6) {
            altKm = LEO_MIN + rng.nextDouble() * (LEO_MAX - LEO_MIN);
        } else if (bandRoll < 9) {
            altKm = MEO_MIN + rng.nextDouble() * (MEO_MAX - MEO_MIN);
        } else {
            altKm = GEO_ALT + rng.nextDouble() * 100.0 - 50.0; // ± 50 km GEO band
        }

        double rOrbit       = RE + altKm;                   // orbital radius (km)
        double meanMotion   = Math.sqrt(MU / (rOrbit * rOrbit * rOrbit)); // rad/s
        double velocityKmS  = Math.sqrt(MU / rOrbit);       // circular orbit speed (km/s)

        double incDeg       = rng.nextDouble() * 120.0;     // 0–120° inclination
        double raanDeg      = rng.nextDouble() * 360.0;     // random RAAN
        double theta0Rad    = rng.nextDouble() * 2 * Math.PI; // initial true anomaly

        String noradId = String.format("MOCK%05d", 90000 + index);
        String name    = "MOCK-SAT-" + (index + 1);

        return new MockSatellite(noradId, name, rOrbit,
                meanMotion, velocityKmS,
                Math.toRadians(incDeg), Math.toRadians(raanDeg),
                theta0Rad);
    }

    // ─── Inner satellite model ────────────────────────────────────────────────

    private static final class MockSatellite {
        final String noradId;
        final String name;
        final double rOrbitKm;       // orbital radius
        final double meanMotionRadS; // ω = sqrt(μ/r³)
        final double velocityKmS;    // v = sqrt(μ/r)
        final double incRad;         // inclination
        final double raanRad;        // right ascension of ascending node
        final double theta0Rad;      // initial true anomaly at t=0

        MockSatellite(String noradId, String name, double rOrbitKm,
                      double meanMotionRadS, double velocityKmS,
                      double incRad, double raanRad, double theta0Rad) {
            this.noradId        = noradId;
            this.name           = name;
            this.rOrbitKm       = rOrbitKm;
            this.meanMotionRadS = meanMotionRadS;
            this.velocityKmS    = velocityKmS;
            this.incRad         = incRad;
            this.raanRad        = raanRad;
            this.theta0Rad      = theta0Rad;
        }

        /**
         * Computes geodetic position at simulation time {@code tSeconds}.
         *
         * @return [latDeg, lonDeg, altKm] or {@code null} if result is invalid
         */
        double[] positionAt(double tSeconds) {
            // 1. True anomaly θ(t) for circular orbit
            double theta = theta0Rad + meanMotionRadS * tSeconds;

            // 2. Position in orbital plane (perifocal frame)
            double xP = rOrbitKm * Math.cos(theta);
            double yP = rOrbitKm * Math.sin(theta);

            // 3. Orbital → ECI rotation (inclination i, RAAN Ω, ω=0 for circular)
            double cosO = Math.cos(raanRad),  sinO = Math.sin(raanRad);
            double cosI = Math.cos(incRad),   sinI = Math.sin(incRad);
            // With ω=0: rotation matrix reduces to
            double xEci = cosO * xP - sinO * cosI * yP;
            double yEci = sinO * xP + cosO * cosI * yP;
            double zEci =             sinI          * yP;

            // 4. ECI → ECEF via GMST (use simulation time relative to Unix epoch)
            // For mock data we use a simplified Earth rotation rate
            double gmstRad = 7.2921150e-5 * (System.currentTimeMillis() / 1_000.0);
            double cosG = Math.cos(gmstRad);
            double sinG = Math.sin(gmstRad);

            double xEcef =  cosG * xEci + sinG * yEci;
            double yEcef = -sinG * xEci + cosG * yEci;
            double zEcef =  zEci;

            // 5. ECEF → WGS-84 geodetic (Bowring, 5 iterations)
            double p   = Math.sqrt(xEcef * xEcef + yEcef * yEcef);
            double lon = Math.atan2(yEcef, xEcef);
            double lat = Math.atan2(zEcef, p * (1 - E2));

            for (int iter = 0; iter < 5; iter++) {
                double sinLat = Math.sin(lat);
                double N      = RE / Math.sqrt(1 - E2 * sinLat * sinLat);
                lat = Math.atan2(zEcef + E2 * N * sinLat, p);
            }

            double sinLat = Math.sin(lat);
            double N      = RE / Math.sqrt(1 - E2 * sinLat * sinLat);
            double altKm  = p / Math.cos(lat) - N;

            if (Double.isNaN(lat) || Double.isNaN(lon) || Double.isNaN(altKm)) return null;

            return new double[]{
                    Math.toDegrees(lat),
                    Math.toDegrees(lon),
                    altKm
            };
        }
    }
}
