# AGENTS.MD — Digital Twin Earth (MIT, Zing-ready)

## 1. Project Overview

**Name:** `digital-twin-earth` / **OpenTracker**  
**Author:** Palaash Atri  
**License:** MIT  
**Goal:** An open-source, production-grade web application that acts as a real-time digital twin of Earth with 3D flight, vessel, and satellite tracking. Architected to stress-test the Azul Zing JVM under realistic production loads.

### Guiding principles

- No proprietary data or API keys committed to the repo.
- All external feeds abstracted behind interfaces, configured via environment variables.
- Default implementation uses deterministic mock/simulated data — runs fully offline.
- No experimental JVM features; Zing-compatible from day one.
- All dependencies must be Apache 2.0, MIT, or BSD licensed — no GPL or AGPL.

---

## 2. Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 17, TypeScript, CesiumJS 1.116, SCSS |
| Gateway | Spring Boot 3.2, Spring MVC, SSE |
| Ingestion | Spring Boot 3.2, Kafka producer, pure-Java TLE propagator |
| Processing | Spring Boot 3.2, Kafka consumer, PostgreSQL |
| Query | Spring Boot 3.2, PostGIS, Redis |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7.2 |
| Messaging | Apache Kafka 3.7 (Confluent images) |
| Build | Gradle 8.7 (Kotlin DSL), version catalog (`libs.versions.toml`) |
| JVM | Java 17 (Azul Zing recommended for benchmarking) |

---

## 3. Architecture

### 3.1 Services

1. **Gateway** (`:8080`) — HTTP routing, CORS, token auth, SSE fan-out to browser clients.
2. **Ingestion** (`:8081`) — Feed providers (mock/OpenSky/AISStream/Celestrak) + Kafka producer. Includes a pure-Java Keplerian SGP4 TLE propagator (`TlePropagator.java`) with no external satellite library dependencies.
3. **Stream Processor** (`:8082`) — Kafka consumer; deduplicates, enriches, writes to PostgreSQL and Redis.
4. **Geospatial** (`:8083`) — PostGIS bounding-box queries, track queries, Redis current-position cache.
5. **Frontend** — Angular SPA served by nginx; connects to Gateway via REST + SSE.

### 3.2 Data Flow

```text
Feeds (mock / real) → Ingestion → Kafka (*.raw)
                                         ↓
                              Stream Processor → PostgreSQL (tracks)
                                              → Redis (current positions)
                                                        ↓
                                              Geospatial ← Gateway ← Browser (SSE)
```

### 3.3 Mock Data System

All three simulators use `new Random(seed)` for fully deterministic output:

- `FlightSimulator` — aircraft on great-circle routes with heading/speed jitter
- `ShipSimulator` — vessels on realistic sea lanes (Atlantic, Pacific, Suez, Malacca, etc.)
- `MockSatelliteProvider` — circular Keplerian orbits; LEO 60% / MEO 30% / GEO 10% distribution

Environment variables: `MOCK_SEED`, `MOCK_FLIGHTS_COUNT`, `MOCK_SHIPS_COUNT`, `MOCK_SATELLITES_COUNT`.

### 3.4 CesiumJS Notes (version 1.116)

- `imageryProvider` constructor option — deprecated 1.103, silently ignored 1.116. Use `baseLayer: new Cesium.ImageryLayer(provider)` in the `Viewer` constructor.
- `imageryLayers.addImageryProvider()` — deprecated 1.103, no-op 1.116.
- `Cesium.Ion.defaultAccessToken = ''` — explicitly disables Ion; `undefined` triggers a silent demo-token fallback that blocks rendering.
- `ArcGISTiledElevationTerrainProvider` requires an API key; using it without one causes per-frame `loadImage` errors and an invisible globe when `depthTestAgainstTerrain = true`.
- Workers are separate files — `CESIUM_BASE_URL` must be set via `<script>` in `index.html` before Angular loads (webpack `DefinePlugin` only covers bundled code).

### 3.5 Jackson Version Compatibility

Spring Boot 3.2.x BOM pins `jackson-core` to ~2.16.x. This project uses `jackson-databind` 2.17.0, which calls `BufferRecycler.releaseToPool()` added in core 2.17.0. The root `build.gradle.kts` forces all three Jackson core modules to 2.17.0 via `resolutionStrategy.force()` to prevent the BOM from downgrading at runtime.

---

## 4. Data Model

### Aircraft

- `icao24`, `callsign`, `airline`, `model`, `country`, `source`
- Position: `lat`, `lon`, `altitudeMeters`, `groundSpeedMps`, `headingDeg`, `verticalRateMps`, `timestamp`

### Vessel

- `mmsi`, `imo`, `name`, `type`, `flag`, `source`
- Position: `lat`, `lon`, `speedKnots`, `courseDeg`, `timestamp`

### Satellite

- `noradId`, `name`, `intlDesignator`, `orbitType` (LEO/MEO/GEO), `source`
- Position: `lat`, `lon`, `altitudeKm`, `velocityKmS`, `timestamp`

---

## 5. API Reference

### REST

| Endpoint | Description |
| --- | --- |
| `GET /api/flights?bbox=minLat,minLon,maxLat,maxLon` | Flight positions in bounding box |
| `GET /api/flights/{id}/track?from=&to=` | Historical track for one aircraft |
| `GET /api/ships?bbox=...` | Vessel positions |
| `GET /api/ships/{id}/track?from=&to=` | Historical track for one vessel |
| `POST /api/scene/query` | Combined scene query (flights + ships) |

### SSE Streams

| Endpoint | Event payload |
| --- | --- |
| `GET /api/stream/flights` | `AircraftPositionDto` |
| `GET /api/stream/vessels` | `VesselPositionDto` |
| `GET /api/stream/satellites` | `SatellitePositionDto` |

All endpoints require `X-Api-Key` header (default: `dev-key`).

---

## 6. Repository Structure

```text
/
  LICENSE
  README.md
  AGENTS.MD                   ← this file
  build.gradle.kts            ← root config: Java target, Jackson version forcing
  settings.gradle.kts
  run.sh                      ← one-command Docker runner

  gradle/
    libs.versions.toml        ← single version catalog

  backend/
    shared/                   ← DTOs, domain events, shared Jackson config
    gateway/                  ← SSE fan-out, public REST, auth          :8080
    ingestion/                ← feed providers, TLE propagator           :8081
    stream-processor/         ← Kafka consumer, DB writer                :8082
    geospatial/               ← PostGIS + Redis query service            :8083

  frontend/
    digital-twin-earth-app/
      src/app/
        globe/                ← GlobeComponent, LayerToggle, Timeline
        services/             ← GlobeEngineService, FlightService, ShipService, SatelliteService
        models/               ← DTOs mirroring backend types

  deploy/
    docker-compose.yml

  docs/
    ARCHITECTURE.md
    API.md
    ZING.md
    ZING_STRESS_TEST.md
    IMAGERY_SETUP.md
    DATA_SOURCES.md
```

---

## 7. Running the Project

```bash
# Mock mode — fully offline, deterministic
./run.sh mock up

# Real feeds
INGESTION_FEED_FLIGHT=opensky \
INGESTION_FEED_SHIP=aisstream \
AISSTREAM_API_KEY=<key> \
./run.sh up

# Stress test
MOCK_FLIGHTS_COUNT=20000 MOCK_SHIPS_COUNT=10000 ./run.sh mock up

# Lifecycle
./run.sh down | logs | status | restart | clean
```

---

## 8. Zing JVM Benchmarking

See `docs/ZING.md` and `docs/ZING_STRESS_TEST.md`. Key points:

- No vendor-specific APIs — only standard JVM flags.
- High-rate ingestion: tune `MOCK_FLIGHTS_COUNT` and `ingestion.sim.interval-ms`.
- k6 scripts provided for REST + SSE load testing.
- Target workloads: 10k–100k msgs/sec ingestion; 32–256 GB heap.

---

## 9. Suggestions & Roadmap

These are improvements I'd recommend implementing, roughly ordered by impact vs. effort.

---

### 9.1 Viewport-aware SSE filtering

**Problem:** The gateway currently streams ALL entities to every connected client regardless of what the camera is looking at. With 500+ flights this is already significant; at 20k it becomes a bandwidth and rendering bottleneck.

**Suggestion:** Add a client-reported bounding box to the SSE subscription (query param: `bbox`). The gateway filters entity updates to only those inside the current viewport. As the user pans/zooms, the Angular app re-issues the subscription with the updated bbox.

This is the single highest-leverage performance improvement — it scales the frontend to arbitrarily large datasets.

---

### 9.2 Entity clustering at low zoom

**Problem:** At global zoom with 500+ aircraft, point entities overlap completely and the globe looks like noise.

**Suggestion:** Implement viewport-aware clustering in `GlobeEngineService`. At altitude > ~8,000 km, replace individual points with cluster billboards showing a count badge. Cesium has a built-in `EntityCluster` API (`viewer.entities.cluster`). Style cluster circles with the same liquid glass aesthetic as the rest of the UI.

---

### 9.3 3D aircraft and vessel models (GLTF)

**Problem:** Flights and ships currently render as colored points. This makes the globe look sparse and doesn't convey heading visually.

**Suggestion:** Replace `point:` with `model:` for entities within a zoom threshold (e.g., altitude < 3,000 km). Use free CC0 GLTF models:

- Aircraft: a simple generic commercial jet silhouette
- Ships: a simple hull shape

Cesium supports orientation-aware GLTF models natively via `entity.orientation` (already set from `headingDeg`). Outside the zoom threshold, fall back to points to keep GPU load bounded.

---

### 9.4 Satellite ground track visualization

**Problem:** Satellites are rendered as single points. Their orbital paths aren't visible, making them hard to understand.

**Suggestion:** For each selected or focused satellite, render a `polyline` showing the next 90 minutes of ground track (one full LEO orbit). The `TlePropagator` already has the propagation math; call `propagate(t)` at 60-second intervals to generate the track points. Show it as a faint dashed line using `Cesium.PolylineGlowMaterialProperty`.

---

### 9.5 Real-time search / geosearch

**Problem:** With hundreds of entities on screen, there's no way to find a specific flight (e.g., "BA123") or jump to a city.

**Suggestion:** Add a search bar to the top bar pill (expanding on focus). Support two query types:

1. **Entity search** — filter flights/ships/satellites by callsign, MMSI, or NORAD ID against the live dataset in memory.
2. **Place search** — use the Nominatim API (OpenStreetMap's geocoder, free, no key) to resolve city/airport names to coordinates, then fly the camera there.

Fly the camera using `viewer.camera.flyTo()` when a result is selected.

---

### 9.6 Track polylines on entity click

**Problem:** Clicking a flight opens the detail panel but shows no historical path. The code scaffolding (`trackEntities` map) exists but the track REST endpoint isn't wired to the UI.

**Suggestion:** When an entity is selected:

1. Call `GET /api/flights/{id}/track?from=now-2h&to=now`.
2. Render the returned positions as a `Cesium.PolylineGraphics` in blue (flights), green (ships), or gold (satellites).
3. Clear the track when the entity is deselected.

This is low-effort since the backend endpoint and the `trackEntities` map are already in place.

---

### 9.7 TimescaleDB for track history

**Problem:** PostgreSQL without TimescaleDB handles time-series data inefficiently at high ingestion rates. Track queries that span hours will get slower as the database grows.

**Suggestion:** Enable the TimescaleDB extension on the existing PostgreSQL 16 container (it's available as `timescale/timescaledb-ha:pg16`). Convert the `aircraft_positions` and `vessel_positions` tables to hypertables. This gives automatic chunk-based time partitioning, compression, and native time-series query optimization — with no application code changes.

The architecture doc already mentions TimescaleDB as desired; it's a two-line schema migration.

---

### 9.8 WebSocket upgrade path

**Problem:** SSE is one-directional. As features grow (time-scrubbing playback sync, collaborative views, admin commands), a bidirectional channel becomes necessary.

**Suggestion:** Introduce a `spring-boot-starter-websocket` adapter behind the same endpoint contract. Keep SSE as the default for simplicity; add WebSocket as an opt-in transport detected via `Upgrade: websocket` header. The Angular `FlightService`/`ShipService` already use RxJS subjects, so the transport swap is transparent to the components.

---

### 9.9 Airport and port layer

**Problem:** The architecture specifies an airports/ports layer but it isn't implemented. Without reference points, the globe feels abstract.

**Suggestion:** Add a static airport layer using the [OurAirports dataset](https://ourairports.com/data/) (public domain CSV, ~74k airports). Load on startup as a lightweight geospatial layer — render large airports as icons at zoom < 2,000 km, medium airports at < 500 km. No backend changes needed; serve the CSV as a static asset and parse it in the Angular layer service.

For ports, use OpenStreetMap Overpass (already used in street view mode) to query `amenity=port` nodes.

---

### 9.10 Keyboard shortcuts

**Problem:** The UI is mouse-centric. Power users and demo scenarios benefit from keyboard control.

**Suggestion:** Add a `KeyboardShortcutService` that listens on `document`:

| Key | Action |
| --- | --- |
| `F` | Toggle flights layer |
| `S` | Toggle ships layer |
| `T` | Toggle satellites layer |
| `H` | Fly to home (global view) |
| `R` | Recover earth view |
| `+` / `-` | Zoom in / out |
| `/` | Focus search bar |
| `Escape` | Close detail panel / deselect entity |

---

### 9.11 Cesium Ion terrain (optional, documented)

**Problem:** The app uses `EllipsoidTerrainProvider` (flat sphere) because terrain providers require paid API keys. This is correct for the open-source default, but the UI currently shows no way to enable terrain.

**Suggestion:** Add an optional `CESIUM_ION_TOKEN` environment variable injected into the Angular app via `environment.ts`. When set:

1. `applyTerrainProvider()` calls `Cesium.createWorldTerrainAsync()`.
2. `depthTestAgainstTerrain = true` is enabled.
3. The terrain toggle in the layer panel becomes active.

Document the setup in `docs/IMAGERY_SETUP.md` (already exists). This keeps the app zero-key by default but gives advanced users terrain without code changes.

---

### 9.12 Helm chart for Kubernetes

**Problem:** The project only ships `docker-compose.yml`. For production-scale Zing benchmarking, Kubernetes is the target environment.

**Suggestion:** Add a `deploy/helm/` chart with:

- One deployment per service (gateway, ingestion, stream-processor, geospatial)
- Kafka via Bitnami Helm dependency
- PostgreSQL via Bitnami Helm dependency
- ConfigMap for environment variables
- HPA (HorizontalPodAutoscaler) on gateway and geospatial for load-test scenarios

The Zing benchmarking story becomes: `helm install opentracker ./deploy/helm --set ingestion.mockFlightsCount=50000`.

---

### 9.13 GeoJSON / CSV export

**Problem:** No way to extract the live dataset for offline analysis or to feed into other tools.

**Suggestion:** Add a `GET /api/export/flights.geojson?bbox=...` endpoint that returns the current snapshot as a GeoJSON FeatureCollection. Add a download button to the stats panel. This is useful for demos and makes the data accessible to GIS tools like QGIS without any additional infrastructure.
