# AGENTS.MD — Digital Twin Earth (MIT, Zing‑ready)

## 1. Project overview

**Name:** `digital-twin-earth`  
**Goal:** Build an open‑source, MIT‑licensed (author Palaash Atri), production‑grade web application that acts as a “digital twin of Earth” with:

- **3D globe** (Google Earth–like) in the browser.
- **Flight tracking** (civilian/commercial; military where data is publicly visible).
- **Ship tracking** (civilian/commercial; military where data is publicly visible).
- **Time‑scrubbing & historical replay** of tracks.
- **Scene/layer concept** (traffic, airports/ports, etc.).

The system must:

- Be **commercially viable** but **fully open source** (MIT).
- Be architected to **stress test Azul Zing (Azul Platform Prime)** under realistic load.
- Be cleanly structured so it can be extended with more layers (weather, airspace, etc.).

> **Important:**  
> - No proprietary data or keys should be committed.  
> - All external feeds must be abstracted behind interfaces and configured via environment variables.  
> - Default implementation may use mock/simulated data so the project runs out‑of‑the‑box.

---

## 2. Tech stack

**Backend:**

- Language: **Java 17+**
- Framework: **Spring Boot 3+**
- Build: **Maven** or **Gradle** (pick one and be consistent; prefer Gradle if undecided).
- Persistence:
  - **PostgreSQL + PostGIS** for geospatial + time‑series (can use TimescaleDB extension if desired).
  - Optional: **Redis** for fast “current positions” cache.
- Messaging (for ingestion pipeline):
  - **Kafka** (preferred) or **RabbitMQ** (pluggable).
- APIs:
  - REST (JSON) as primary.
  - WebSocket or Server‑Sent Events (SSE) for live updates.
- JVM target:
  - Must run cleanly on **Azul Zing** with no vendor‑specific code.
  - Provide **GC/heap tuning examples** for Zing in docs.

**Frontend:**

- **Angular 17+**
- TypeScript, RxJS.
- 3D globe:
  - Prefer **CesiumJS** (Apache 2.0) integrated into Angular.
  - Keep globe engine abstracted so Web WorldWind or others could be swapped later.

**Licensing:**

- Root project: **MIT License**.
- Ensure all dependencies are compatible with commercial use.
- No GPL or AGPL dependencies.

---

## 3. High‑level architecture

### 3.1 Services

1. **Gateway/API service (Spring Boot)**
   - Handles:
     - HTTP routing to internal services.
     - CORS, auth (basic token‑based for now), rate limiting (simple).
   - Exposes:
     - `/api/flights/...`
     - `/api/ships/...`
     - `/api/scene/...`
     - `/api/stream/...` (WebSocket/SSE).

2. **Ingestion service(s) (Spring Boot)**
   - Separate module(s) for:
     - **Flight ingestion** (ADS‑B–like feeds).
     - **Ship ingestion** (AIS–like feeds).
   - Responsibilities:
     - Poll or subscribe to external feeds (or mock generators).
     - Normalize into internal DTOs.
     - Publish to Kafka topics: `flights.raw`, `ships.raw`.

3. **Stream processor service (Spring Boot)**
   - Consumes `flights.raw`, `ships.raw`.
   - Responsibilities:
     - Deduplicate, smooth positions.
     - Compute derived fields (ground speed, climb rate, etc.).
     - Maintain:
       - **Current positions** (Redis or Postgres table).
       - **Historical tracks** (Postgres/TimescaleDB).
     - Publish processed events to `flights.processed`, `ships.processed` if needed.

4. **Geospatial/query service (Spring Boot)**
   - Wraps PostGIS/TimescaleDB.
   - Responsibilities:
     - Bounding‑box queries for current positions.
     - Track queries for a given entity and time range.
     - Aggregations (counts, density, etc.).
   - Exposed via REST to the gateway.

5. **Frontend (Angular)**
   - Single‑page app served by backend or static hosting.
   - Integrates CesiumJS for 3D globe.
   - Connects to:
     - REST endpoints for snapshots and tracks.
     - WebSocket/SSE for live updates.

### 3.2 Data flow (simplified)

1. External feeds (or mock generators) → Ingestion service → Kafka (`*.raw`).
2. Stream processor → cleans & enriches → DB (PostGIS/TimescaleDB) + cache.
3. Geospatial service → queries DB/cache → returns JSON.
4. Gateway → exposes unified API to frontend.
5. Angular app → renders entities on 3D globe + time controls.

---

## 4. Data model

### 4.1 Core entities

**Aircraft**

- `id` (UUID)
- `icao24` (string)
- `callsign` (string, nullable)
- `airline` (string, nullable)
- `model` (string, nullable)
- `country` (string, nullable)
- `source` (enum: `ADS_B`, `SIMULATED`, `PRIVATE_FEED`, etc.)

**AircraftPosition**

- `id` (UUID)
- `aircraft_id` (FK → Aircraft)
- `timestamp` (UTC, instant)
- `lat` (double)
- `lon` (double)
- `altitude_m` (double, nullable)
- `ground_speed_mps` (double, nullable)
- `heading_deg` (double, nullable)
- `vertical_rate_mps` (double, nullable)

**Vessel**

- `id` (UUID)
- `mmsi` (string)
- `imo` (string, nullable)
- `name` (string, nullable)
- `type` (string, nullable)
- `flag` (string, nullable)
- `source` (enum: `AIS`, `SIMULATED`, `PRIVATE_FEED`, etc.)

**VesselPosition**

- `id` (UUID)
- `vessel_id` (FK → Vessel)
- `timestamp` (UTC)
- `lat` (double)
- `lon` (double)
- `speed_knots` (double, nullable)
- `course_deg` (double, nullable)

### 4.2 Scene & layers

**SceneLayer**

- `id` (string, e.g. `flights`, `ships`, `airports`, `ports`)
- `type` (enum: `VECTOR`, `RASTER`, `ENTITY`)
- `enabled` (bool)

**SceneQuery**

- Bounding box: `minLat`, `maxLat`, `minLon`, `maxLon`
- Time range: `from`, `to` (optional; `now` for live snapshot)
- Layers: list of layer IDs
- Filters: optional (airline, vessel type, speed/altitude ranges, etc.)

---

## 5. API design

### 5.1 REST endpoints (examples)

**Flights**

- `GET /api/flights`
  - Query params: `bbox`, `time` (optional, default `now`), pagination.
  - Returns: list of current or time‑slice positions.

- `GET /api/flights/{id}`
  - Returns: aircraft metadata.

- `GET /api/flights/{id}/track`
  - Query params: `from`, `to`.
  - Returns: ordered list of positions.

**Ships**

- `GET /api/ships`
- `GET /api/ships/{id}`
- `GET /api/ships/{id}/track`

**Scene**

- `POST /api/scene/query`
  - Body: `SceneQuery`.
  - Returns: aggregated scene data (flights, ships, etc.) in a single payload.

### 5.2 Streaming endpoints

- `GET /api/stream/flights` (SSE or WebSocket)
  - Streams incremental updates (create/update/delete) for aircraft positions.

- `GET /api/stream/ships`
  - Same for vessels.

> Implement SSE first (simpler), but keep abstraction so WebSocket can be added later.

---

## 6. Frontend requirements (Angular + Cesium)

### 6.1 Core features

- 3D globe component:
  - Pan/zoom/tilt.
  - Base imagery (e.g. Cesium ion or open imagery; **do not hardcode keys**).
  - Terrain optional (configurable).

- Layers:
  - Toggle flights, ships, and any future layers.
  - Legend and simple filters (e.g. airline, vessel type).

- Live mode:
  - Subscribe to SSE/WebSocket.
  - Smoothly update entity positions on the globe.

- Time‑scrubbing:
  - Timeline UI (from/to).
  - Request historical tracks and render as polylines.
  - Playback controls (play/pause, speed multiplier).

### 6.2 Angular structure

- `app/modules/globe`:
  - `GlobeComponent` (hosts Cesium canvas).
  - `LayerToggleComponent`.
  - `TimelineComponent`.

- `app/services`:
  - `FlightService` (REST + SSE/WebSocket).
  - `ShipService`.
  - `SceneService` (higher‑level orchestration).

- Use RxJS streams for live updates and state management.

---

## 7. Zing JVM stress‑testing focus

The application must be structured so it can be used as a **Zing benchmark**.

### 7.1 Workload characteristics to support

- High‑rate ingestion (configurable target, e.g. 10k–100k messages/sec).
- Bursty traffic patterns (simulated if needed).
- Large heap usage (document how to run with 32–256 GB heaps).
- High object churn in:
  - Ingestion pipeline.
  - Stream processing.
  - Geospatial queries.
  - JSON serialization/deserialization.

### 7.2 Zing‑specific documentation

Create a `docs/ZING.md` with:

- Example JVM options for Zing (heap size, GC logging).
- How to:
  - Run ingestion at high rates.
  - Run load tests against REST and streaming endpoints.
  - Observe latency and GC behavior.

No Zing‑specific APIs—only configuration and docs.

---

## 8. Repository structure

Target structure (agents should create and keep consistent):

```text
/ (root)
  LICENSE              # MIT
  README.md
  AGENTS.MD            # This file
  /backend
    /gateway
    /ingestion
    /stream-processor
    /geospatial
    build.gradle[.kts] or pom.xml
  /frontend
    /digital-twin-earth-app
  /deploy
    docker-compose.yml
    k8s/ (optional)
  /docs
    ARCHITECTURE.md
    API.md
    ZING.md
