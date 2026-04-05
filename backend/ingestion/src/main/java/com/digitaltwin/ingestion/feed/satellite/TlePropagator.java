package com.digitaltwin.ingestion.feed.satellite;

/**
 * Pure-Java Keplerian SGP4 propagator — no external library dependencies.
 *
 * <p>Implements the math documented in docs/DATA_SOURCES.md and SATELLITES.md:
 * <ol>
 *   <li>TLE text parsing (3-line format: name / line1 / line2)</li>
 *   <li>Newton-Raphson Kepler equation solver</li>
 *   <li>Perifocal (PQW) → Earth-Centered Inertial (ECI) rotation</li>
 *   <li>ECI → Earth-Centered Earth-Fixed (ECEF) via GMST (IAU 1982)</li>
 *   <li>ECEF → WGS-84 geodetic latitude/longitude/altitude (Bowring iterative, 5 iters)</li>
 *   <li>Orbital velocity approximation via vis-viva equation</li>
 * </ol>
 *
 * <p>This is a simplified Keplerian (two-body) propagator.  It does not model
 * atmospheric drag, J2 oblateness, or solar radiation pressure, but is
 * sufficient for real-time position display purposes.
 */
public final class TlePropagator {

    // ─── WGS-84 constants ────────────────────────────────────────────────────

    /** Earth gravitational parameter μ (km³ s⁻²). */
    private static final double MU  = 398_600.4418;
    /** WGS-84 semi-major axis (km). */
    private static final double RE  = 6_378.137;
    /** WGS-84 inverse flattening → flattening f = 1/298.257223563. */
    private static final double F   = 1.0 / 298.257_223_563;
    /** WGS-84 first eccentricity squared e² = 2f − f². */
    private static final double E2  = 2 * F - F * F;

    // ─── GMST (IAU 1982) coefficients ────────────────────────────────────────

    /** GMST at J2000.0 (degrees). */
    private static final double GMST0_DEG  = 280.460_618_37;
    /** Mean Earth rotation rate (degrees per solar day). */
    private static final double GMST_RATE  = 360.985_647_366_29;
    /** Julian Day at Unix epoch (1970-01-01 00:00:00 UTC). */
    private static final double JD_UNIX    = 2_440_587.5;
    /** Julian Day at J2000.0 (2000-01-01 12:00:00 UTC). */
    private static final double JD_J2000   = 2_451_545.0;

    // ─── Newton-Raphson convergence ───────────────────────────────────────────

    private static final int NR_ITERATIONS = 10;

    // ─── Parsed TLE orbital elements ─────────────────────────────────────────

    /** Satellite name (TLE line 0). */
    public final String name;
    /** NORAD catalogue number (5 digits from TLE line 1, columns 3-7). */
    public final String noradId;

    /** Semi-major axis derived from mean motion (km). */
    private final double semiMajorKm;
    /** Orbital eccentricity (dimensionless). */
    private final double eccentricity;
    /** Inclination (radians). */
    private final double inclinationRad;
    /** Right ascension of ascending node Ω (radians). */
    private final double raanRad;
    /** Argument of perigee ω (radians). */
    private final double argPerigeeRad;
    /** Mean anomaly at epoch M₀ (radians). */
    private final double meanAnomalyRad;
    /** Mean motion n (radians per second). */
    private final double meanMotionRadS;
    /** TLE epoch as Unix timestamp (milliseconds). */
    private final long epochMs;

    private TlePropagator(String name, String noradId,
                          double semiMajorKm, double eccentricity,
                          double inclinationRad, double raanRad,
                          double argPerigeeRad, double meanAnomalyRad,
                          double meanMotionRadS, long epochMs) {
        this.name            = name;
        this.noradId         = noradId;
        this.semiMajorKm     = semiMajorKm;
        this.eccentricity    = eccentricity;
        this.inclinationRad  = inclinationRad;
        this.raanRad         = raanRad;
        this.argPerigeeRad   = argPerigeeRad;
        this.meanAnomalyRad  = meanAnomalyRad;
        this.meanMotionRadS  = meanMotionRadS;
        this.epochMs         = epochMs;
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    /**
     * Parses a 3-element TLE (name, line1, line2) and returns a propagator.
     *
     * @param name  satellite name (TLE line 0, may be blank for 2-line TLEs)
     * @param line1 TLE line 1
     * @param line2 TLE line 2
     * @return configured propagator, or {@code null} if parsing fails
     */
    public static TlePropagator parse(String name, String line1, String line2) {
        try {
            // ── NORAD ID (line1 cols 2-7, 0-indexed) ────────────────────────
            String noradId = line1.substring(2, 7).trim();

            // ── Epoch (line1 cols 18-32) ─────────────────────────────────────
            // Format: YYddd.dddddddd  (2-digit year + fractional day of year)
            String epochStr = line1.substring(18, 32).trim();
            long   epochMs  = parseEpoch(epochStr);

            // ── Inclination (line2 cols 8-16) ───────────────────────────────
            double incDeg = Double.parseDouble(line2.substring(8, 16).trim());

            // ── RAAN (line2 cols 17-25) ──────────────────────────────────────
            double raanDeg = Double.parseDouble(line2.substring(17, 25).trim());

            // ── Eccentricity (line2 cols 26-33, implied decimal: .NNNNNNN) ───
            double ecc = Double.parseDouble("0." + line2.substring(26, 33).trim());

            // ── Argument of perigee (line2 cols 34-42) ───────────────────────
            double argPerDeg = Double.parseDouble(line2.substring(34, 42).trim());

            // ── Mean anomaly (line2 cols 43-51) ──────────────────────────────
            double m0Deg = Double.parseDouble(line2.substring(43, 51).trim());

            // ── Mean motion (line2 cols 52-63, rev/day) ──────────────────────
            double mmRevDay = Double.parseDouble(line2.substring(52, 63).trim());

            // Convert mean motion: rev/day → rad/s
            double mmRadS = mmRevDay * 2.0 * Math.PI / 86_400.0;

            // Derive semi-major axis from Kepler's 3rd law: a = (μ/n²)^(1/3)
            double a = Math.cbrt(MU / (mmRadS * mmRadS));

            return new TlePropagator(
                    name.isEmpty() ? "SAT-" + noradId : name,
                    noradId,
                    a, ecc,
                    Math.toRadians(incDeg),
                    Math.toRadians(raanDeg),
                    Math.toRadians(argPerDeg),
                    Math.toRadians(m0Deg),
                    mmRadS,
                    epochMs
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Propagation ──────────────────────────────────────────────────────────

    /**
     * Propagates the satellite to the given Unix timestamp and returns its
     * geodetic position and orbital speed.
     *
     * @param unixMs target epoch (milliseconds since 1970-01-01 UTC)
     * @return {@link Position} with lat/lon/alt/velocity, or {@code null} if propagation fails
     */
    public Position propagate(long unixMs) {
        try {
            // ── 1. Time since epoch (seconds) ─────────────────────────────────
            double dt = (unixMs - epochMs) / 1_000.0;

            // ── 2. Mean anomaly at target time ────────────────────────────────
            double M = meanAnomalyRad + meanMotionRadS * dt;
            M = normalizeAngle(M);

            // ── 3. Eccentric anomaly via Newton-Raphson ───────────────────────
            double E = solveKepler(M, eccentricity);

            // ── 4. True anomaly ν ─────────────────────────────────────────────
            double sinV = Math.sqrt(1 - eccentricity * eccentricity) * Math.sin(E);
            double cosV = Math.cos(E) - eccentricity;
            double nu   = Math.atan2(sinV, cosV);

            // ── 5. Orbital radius ─────────────────────────────────────────────
            double r = semiMajorKm * (1 - eccentricity * Math.cos(E));

            // ── 6. Position in perifocal (PQW) frame ─────────────────────────
            double xP = r * Math.cos(nu);
            double yP = r * Math.sin(nu);

            // ── 7. PQW → ECI (rotation by ω, i, Ω) ───────────────────────────
            double omega = argPerigeeRad;
            double inc   = inclinationRad;
            double raan  = raanRad;

            double cosO = Math.cos(raan),   sinO = Math.sin(raan);
            double cosI = Math.cos(inc),    sinI = Math.sin(inc);
            double cosW = Math.cos(omega),  sinW = Math.sin(omega);

            // Standard rotation matrix R = Rz(-Ω) · Rx(-i) · Rz(-ω)
            double r11 =  cosO * cosW - sinO * sinW * cosI;
            double r12 = -cosO * sinW - sinO * cosW * cosI;
            double r21 =  sinO * cosW + cosO * sinW * cosI;
            double r22 = -sinO * sinW + cosO * cosW * cosI;
            double r31 =  sinW * sinI;
            double r32 =  cosW * sinI;

            double xEci = r11 * xP + r12 * yP;
            double yEci = r21 * xP + r22 * yP;
            double zEci = r31 * xP + r32 * yP;

            // ── 8. ECI → ECEF via GMST (IAU 1982) ────────────────────────────
            double gmst = gmstRadians(unixMs);
            double cosG = Math.cos(gmst);
            double sinG = Math.sin(gmst);

            double xEcef =  cosG * xEci + sinG * yEci;
            double yEcef = -sinG * xEci + cosG * yEci;
            double zEcef =  zEci;

            // ── 9. ECEF → WGS-84 geodetic (Bowring iterative, 5 iterations) ──
            double p      = Math.sqrt(xEcef * xEcef + yEcef * yEcef); // distance from rotation axis
            double lon    = Math.atan2(yEcef, xEcef);
            double lat    = Math.atan2(zEcef, p * (1 - E2));           // initial estimate

            for (int iter = 0; iter < 5; iter++) {
                double sinLat = Math.sin(lat);
                double N      = RE / Math.sqrt(1 - E2 * sinLat * sinLat);
                lat = Math.atan2(zEcef + E2 * N * sinLat, p);
            }

            double sinLat = Math.sin(lat);
            double N      = RE / Math.sqrt(1 - E2 * sinLat * sinLat);
            double alt    = p / Math.cos(lat) - N;           // altitude above ellipsoid (km)

            // Sanity check
            if (alt < -100 || Double.isNaN(lat) || Double.isNaN(lon) || Double.isNaN(alt)) {
                return null;
            }

            // ── 10. Vis-viva velocity approximation (circular orbit at r) ─────
            double velocity = Math.sqrt(MU / r);   // km/s

            return new Position(
                    Math.toDegrees(lat),
                    Math.toDegrees(lon),
                    alt,
                    velocity
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Solves the Kepler equation M = E − e·sin(E) for E using Newton-Raphson.
     *
     * @param M mean anomaly (radians)
     * @param e eccentricity
     * @return eccentric anomaly E (radians)
     */
    private static double solveKepler(double M, double e) {
        double E = M;   // initial guess
        for (int i = 0; i < NR_ITERATIONS; i++) {
            double dE = (M - E + e * Math.sin(E)) / (1 - e * Math.cos(E));
            E += dE;
            if (Math.abs(dE) < 1e-12) break;
        }
        return E;
    }

    /**
     * Greenwich Mean Sidereal Time (IAU 1982 model) in radians.
     *
     * @param unixMs Unix timestamp in milliseconds
     * @return GMST in radians [0, 2π)
     */
    private static double gmstRadians(long unixMs) {
        double jd       = unixMs / 86_400_000.0 + JD_UNIX;
        double T        = jd - JD_J2000;                // Julian centuries from J2000
        double gmstDeg  = GMST0_DEG + GMST_RATE * T;
        return Math.toRadians(gmstDeg % 360.0);
    }

    /**
     * Parses a TLE epoch string of the form {@code YYddd.dddddddd} into
     * a Unix timestamp in milliseconds.
     *
     * <p>Year pivot: 2-digit years 57–99 → 1957–1999; 00–56 → 2000–2056.
     *
     * @param epochStr raw epoch field from TLE line 1
     * @return Unix epoch milliseconds
     */
    private static long parseEpoch(String epochStr) {
        int    twoDigitYear = (int) Double.parseDouble(epochStr.substring(0, 2));
        int    year         = twoDigitYear >= 57 ? 1900 + twoDigitYear : 2000 + twoDigitYear;
        double dayOfYear    = Double.parseDouble(epochStr.substring(2));

        // Day 1.0 = Jan 1 00:00:00 UTC
        long   jan1Ms       = java.time.LocalDate.of(year, 1, 1)
                                 .atStartOfDay(java.time.ZoneOffset.UTC)
                                 .toInstant()
                                 .toEpochMilli();
        long   dayMs        = (long) ((dayOfYear - 1) * 86_400_000L);
        return jan1Ms + dayMs;
    }

    /** Normalises an angle to [0, 2π). */
    private static double normalizeAngle(double rad) {
        double r = rad % (2 * Math.PI);
        return r < 0 ? r + 2 * Math.PI : r;
    }

    // ─── Result type ──────────────────────────────────────────────────────────

    /** Propagated geodetic position of a satellite. */
    public static final class Position {
        /** Geodetic latitude (degrees, −90 to +90). */
        public final double latDeg;
        /** Geodetic longitude (degrees, −180 to +180). */
        public final double lonDeg;
        /** Altitude above WGS-84 ellipsoid (km). */
        public final double altKm;
        /** Approximate orbital speed via vis-viva (km/s). */
        public final double velocityKmS;

        Position(double latDeg, double lonDeg, double altKm, double velocityKmS) {
            this.latDeg      = latDeg;
            this.lonDeg      = lonDeg;
            this.altKm       = altKm;
            this.velocityKmS = velocityKmS;
        }
    }
}
