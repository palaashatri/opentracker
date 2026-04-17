# DATA_SOURCES.md
Real and mock data sources for Digital Twin Earth

---

## Flights (ADS-B)

### OpenSky Network (default real feed, no key required)
- **URL:** `https://opensky-network.org/api/states/all`
- **License:** Creative Commons BY 4.0
- **Rate:** 10 req/min anonymous; register at opensky-network.org for 100 req/min
- **Enable:** `INGESTION_FEED_FLIGHT=opensky`
- **Optional auth:** `OPENSKY_USERNAME=...` `OPENSKY_PASSWORD=...`
- **Fields used:** icao24, callsign, origin_country, longitude, latitude, baro_altitude, velocity, true_track, vertical_rate

### ADSBExchange (alternative, no key for public endpoint)
- **URL:** `https://public-api.adsbexchange.com/VirtualRadar/AircraftList.json`
- **License:** Open Data — ADSBExchange
- **Enable:** `INGESTION_FEED_FLIGHT=adsb`
- **Fields:** Icao, Call, Lat, Long, Alt (ft), Spd (knots), Trak (deg), Vsi (fpm)

### Mock (default, no network required)
- **Enable:** `INGESTION_FEED_FLIGHT=mock` (default)
- 500 aircraft on great-circle routes between real waypoints
- Deterministic seed: `MOCK_SEED=42`

---

## Ships (AIS)

### AISStream.io (WebSocket, free key required)
- **URL:** `wss://stream.aisstream.io/v0/stream`
- **License:** Open — AISStream.io (register for free key)
- **Enable:** `INGESTION_FEED_SHIP=aisstream` + `AISSTREAM_API_KEY=<key>`
- **Get key:** https://aisstream.io

### AISHub (HTTP poll, free username required)
- **URL:** `https://data.aishub.net/ws.php`
- **License:** Open — AISHub (register for free username)
- **Enable:** `INGESTION_FEED_SHIP=aishub` + `AISHUB_USERNAME=<username>`
- **Get username:** https://www.aishub.net/join-us
- **Fields:** MMSI, LAT, LON, SOG (knots), COG (deg), SHIPNAME, SHIPTYPE, IMO

### Mock (default)
- **Enable:** `INGESTION_FEED_SHIP=mock` (default)
- 200 vessels on major sea lanes (North Atlantic, Suez, Malacca, etc.)
- Deterministic seed: `MOCK_SEED=42`

---

## Satellites (TLE + SGP4)

### Celestrak (free, no key required)
- **URL:** `https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=tle`
- **License:** Public domain (US government data)
- **Enable:** `SATELLITES_ENABLED=true`
- **Refresh cadence:** every 3 hours
- **Data:** ~10,000 active satellites with NORAD IDs, names, TLE lines
- **Propagation:** Custom SGP4 propagator (Keplerian + GMST rotation, WGS-84 geodetic)
- **Update rate:** positions propagated every second

**SGP4 implementation notes:**
- No external library — pure Java Keplerian propagation
- Kepler equation solved by Newton-Raphson (5 iterations, typically converges in 2)
- ECI → ECEF rotation using Greenwich Mean Sidereal Time (IAU 1982 model)
- ECEF → geodetic using Bowring iterative method (5 iterations, mm accuracy)
- Orbital velocity from vis-viva equation: `v = sqrt(μ * (2/r - 1/a))`

### Mock satellites
- **Enable:** `SATELLITES_ENABLED=false` (default local runner behavior)
- N satellites in random circular orbits (LEO/MEO/GEO bands)
- Two-body circular orbit math, deterministic seed

---

## Earth Imagery

### OpenStreetMap (default, no key required)
- **URL:** `https://tile.openstreetmap.org/{z}/{x}/{y}.png`
- **License:** ODbL (Open Database License)
- **Features:** Political boundaries, roads, cities, labels

### Cesium World Terrain (free, no key for basic)
- **License:** Cesium ion terms (free tier for open-source use)
- **Features:** High-resolution elevation data for realistic terrain rendering

---

## Configuration reference

All feeds switch at runtime without code changes:

```bash
# Full real-data demo (best for demos)
INGESTION_FEED_FLIGHT=opensky \
INGESTION_FEED_SHIP=aisstream \
AISSTREAM_API_KEY=your-key \
SATELLITES_ENABLED=true \
./run.sh up

# No internet required (CI/offline demo)
./run.sh

# High-load Zing stress test
SPRING_PROFILES_ACTIVE=mock \
MOCK_FLIGHTS_COUNT=20000 \
MOCK_SHIPS_COUNT=10000 \
MOCK_SATELLITES_COUNT=5000 \
./run.sh up
```

---

## Licensing summary

| Source | License | Key required |
|---|---|---|
| OpenSky Network | CC BY 4.0 | No (optional auth for higher rate) |
| ADSBExchange | Open Data | No |
| AISStream.io | Open | Yes (free) |
| AISHub | Open | Yes (free) |
| Celestrak TLEs | Public domain | No |
| OpenStreetMap | ODbL | No |
| Cesium World Terrain | Cesium ion (free tier) | No |

All sources are MIT-compatible for an open-source project. No proprietary keys are committed.
