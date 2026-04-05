# OpenTracker — Digital Twin Earth

An open-source, MIT-licensed real-time 3D Earth digital twin with live flight, vessel, and satellite tracking. Built to stress-test the Azul Zing JVM under realistic production loads.

**Author:** Palaash Atri  
**License:** MIT

---

## Features

- **3D Globe** — CesiumJS-powered interactive Earth with pan/zoom/tilt and atmosphere
- **Live Flight Tracking** — ADS-B feed (OpenSky Network) or deterministic mock simulation
- **Live Ship Tracking** — AIS feed (AISStream.io) or deterministic mock simulation
- **Live Satellite Tracking** — Celestrak TLE propagation (real) or mock orbital simulation
- **Real-time streaming** — Server-Sent Events push updates directly to the browser
- **Layer system** — Toggle flights, ships, satellites, terrain, buildings, and weather independently
- **Entity detail panel** — Click any entity for full metadata
- **3D Street View mode** — Ground-level camera with nearby Wikimedia street photos
- **Zoom controls** — In/out, home reset, and emergency view-recovery
- **Apple HIG UI** — Liquid glass pill components, smooth spring animations, dark-first aesthetic
- **Mock mode** — Full deterministic simulation; no internet or API keys required
- **Zing-ready** — No experimental JVM features; GC tuning documented in `docs/ZING.md`

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 17, TypeScript, CesiumJS 1.116, SCSS |
| Gateway | Spring Boot 3.2, Spring MVC, SSE |
| Ingestion | Spring Boot 3.2, Kafka producer, TLE propagator |
| Processing | Spring Boot 3.2, Kafka consumer, PostgreSQL |
| Query | Spring Boot 3.2, PostGIS, Redis |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7.2 |
| Messaging | Apache Kafka 3.7 (Confluent) |
| Build | Gradle 8.7 (Kotlin DSL) |
| JVM | Java 17+ (Azul Zing recommended for benchmarking) |

---

## Quick Start

### Prerequisites

- Docker Desktop (with Docker Compose v2)
- `bash` (macOS / Linux)

No API keys needed in mock mode.

### Run in mock mode (no internet required)

```bash
./run.sh mock up
```

Wait ~90 seconds for all services to initialize. Then open:

- **Frontend:** <http://localhost:4200>
- **API:** <http://localhost:8080/api/flights>
- **Health:** <http://localhost:8080/actuator/health>
- **Stream (test):** `curl -H 'X-Api-Key: dev-key' http://localhost:8080/api/stream/flights`

### Run with real data feeds

```bash
INGESTION_FEED_FLIGHT=opensky \
INGESTION_FEED_SHIP=aisstream \
AISSTREAM_API_KEY=your-key \
./run.sh up
```

### Other commands

```bash
./run.sh down          # stop all containers
./run.sh logs          # follow logs from all services
./run.sh status        # show container health
./run.sh restart       # stop then start
./run.sh clean         # remove containers + volumes (destructive)
./run.sh help          # full usage
```

### Stress-test load

```bash
MOCK_FLIGHTS_COUNT=20000 \
MOCK_SHIPS_COUNT=10000 \
MOCK_SATELLITES_COUNT=500 \
./run.sh mock up
```

---

## Configuration

All configuration is environment-variable driven. No keys are committed.

### Mock / simulation

| Variable | Default | Description |
| --- | --- | --- |
| `MOCK_SEED` | `42` | Random seed — same seed = same positions every run |
| `MOCK_FLIGHTS_COUNT` | `500` | Number of simulated aircraft |
| `MOCK_SHIPS_COUNT` | `200` | Number of simulated vessels |
| `MOCK_SATELLITES_COUNT` | `100` | Number of simulated satellites |

### Real data feeds

| Variable | Default | Description |
| --- | --- | --- |
| `INGESTION_FEED_FLIGHT` | `mock` | `mock`, `opensky`, or `adsb` |
| `INGESTION_FEED_SHIP` | `mock` | `mock`, `aisstream`, or `aishub` |
| `SATELLITES_ENABLED` | `true` | `true` = Celestrak TLE feed; `false` = mock |
| `AISSTREAM_API_KEY` | — | Required when `INGESTION_FEED_SHIP=aisstream` |
| `OPENSKY_USERNAME` | — | OpenSky credential (increases rate limit) |
| `OPENSKY_PASSWORD` | — | OpenSky credential |

### Infrastructure

| Variable | Default | Description |
| --- | --- | --- |
| `GATEWAY_API_KEY` | `dev-key` | API authentication key |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Kafka brokers (internal Docker) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/digitaltwin` | PostgreSQL |
| `SPRING_REDIS_HOST` | `redis` | Redis host |

---

## Local Development

### Backend

```bash
# Start infrastructure only
docker compose -f deploy/docker-compose.yml up postgres redis kafka zookeeper -d

# Run any service locally (Java 17 required)
./gradlew :backend:gateway:bootRun
./gradlew :backend:ingestion:bootRun
./gradlew :backend:stream-processor:bootRun
./gradlew :backend:geospatial:bootRun

# Build all JARs (skip tests)
./gradlew build -x test
```

### Frontend

```bash
cd frontend/digital-twin-earth-app
npm install
npm start    # http://localhost:4200, proxies /api → localhost:8080
```

The dev server uses `proxy.conf.json` to forward all `/api` requests to the backend gateway.

---

## Repository Structure

```
/
  LICENSE
  README.md
  AGENTS.MD                 ← project specification and roadmap
  build.gradle.kts          ← root Gradle config (version forcing, Java target)
  settings.gradle.kts
  run.sh                    ← one-command local runner
  gradle/
    libs.versions.toml      ← single version catalog

  backend/
    shared/                 ← DTOs, domain events, shared config
    gateway/                ← public API + SSE fan-out            :8080
    ingestion/              ← feed providers + Kafka producer      :8081
    stream-processor/       ← Kafka consumer + DB writer           :8082
    geospatial/             ← PostGIS + Redis query service        :8083

  frontend/
    digital-twin-earth-app/ ← Angular 17 SPA (CesiumJS 1.116)

  deploy/
    docker-compose.yml

  docs/
    ARCHITECTURE.md         ← system design and data flow
    API.md                  ← REST + SSE endpoint reference
    ZING.md                 ← Azul Zing JVM tuning guide
    ZING_STRESS_TEST.md     ← k6 load test scripts
    IMAGERY_SETUP.md        ← CesiumJS imagery provider reference
    DATA_SOURCES.md         ← external data feed guide
```

---

## API Summary

| Endpoint | Auth | Description |
| --- | --- | --- |
| `GET /api/flights?bbox=...` | `X-Api-Key` | Current flight positions in bounding box |
| `GET /api/flights/{id}/track?from=&to=` | `X-Api-Key` | Historical track for one aircraft |
| `GET /api/ships?bbox=...` | `X-Api-Key` | Current vessel positions |
| `GET /api/ships/{id}/track?from=&to=` | `X-Api-Key` | Historical track for one vessel |
| `POST /api/scene/query` | `X-Api-Key` | Combined scene query |
| `GET /api/stream/flights` | `X-Api-Key` | SSE stream — live flight updates |
| `GET /api/stream/vessels` | `X-Api-Key` | SSE stream — live vessel updates |
| `GET /api/stream/satellites` | `X-Api-Key` | SSE stream — live satellite updates |

See [docs/API.md](docs/API.md) for the full reference.

---

## Zing Stress Testing

See [docs/ZING.md](docs/ZING.md) and [docs/ZING_STRESS_TEST.md](docs/ZING_STRESS_TEST.md) for:

- JVM flag examples for large heaps (32–256 GB)
- Ingestion rate scaling (up to 100k msgs/sec)
- k6 load test scripts for REST and SSE endpoints
- GC log analysis with Zing C4

Quick high-rate example:

```bash
java \
  -Xmx32g -Xms32g \
  -XX:+AlwaysPreTouch \
  -jar backend/ingestion/build/libs/ingestion.jar \
  --mock.flights-count=10000 \
  --mock.ships-count=5000 \
  --spring.profiles.active=mock
```

---

## License

MIT — see [LICENSE](LICENSE).

All dependencies are Apache 2.0, MIT, or BSD licensed. No GPL or AGPL dependencies.
