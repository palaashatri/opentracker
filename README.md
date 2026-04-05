# Digital Twin Earth

An open-source, MIT-licensed 3D Earth digital twin with live flight and ship tracking. Built to stress-test the Azul Zing JVM under realistic production loads.

**Author:** Palaash Atri  
**License:** MIT

## Features

- **3D Globe** — CesiumJS-powered interactive Earth with pan/zoom/tilt
- **Live Flight Tracking** — ADS-B feed simulation (500+ aircraft by default)
- **Live Ship Tracking** — AIS feed simulation (200+ vessels by default)
- **Real-time streaming** — Server-Sent Events push updates to the browser
- **Historical replay** — Time-scrubbing and track polylines
- **Layer system** — Toggle flights, ships, and future layers independently
- **Apple HIG UI** — Liquid glass design, smooth animations, dark-first aesthetic
- **Zing-ready** — No experimental JVM features; GC tuning documented in `docs/ZING.md`

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17, TypeScript, CesiumJS, SCSS |
| Gateway | Spring Boot 3.2, Spring MVC, SSE |
| Ingestion | Spring Boot 3.2, Kafka producer |
| Processing | Spring Boot 3.2, Kafka consumer, PostgreSQL |
| Query | Spring Boot 3.2, PostGIS, Redis |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7.2 |
| Messaging | Apache Kafka 3.7 |
| Build | Gradle 8.7 (Kotlin DSL) |
| JVM | Java 17+ (Azul Zing recommended for benchmarking) |

## Quick Start

### Prerequisites

- Docker + Docker Compose
- Java 17+ (JDK, for local development)
- Node.js 18+ and Angular CLI 17+ (for frontend development)

### Run with Docker Compose

```bash
cd deploy
docker compose up
```

Wait ~60 seconds for all services to initialize. Then open:

- **Frontend:** http://localhost:4200
- **API:** http://localhost:8080
- **Gateway health:** http://localhost:8080/actuator/health

### Local Development

**Backend:**

```bash
# Requires JDK 17+
# Start dependencies only
cd deploy && docker compose up postgres redis kafka zookeeper

# Run any service locally
JAVA_HOME=/path/to/jdk17 ./gradlew :backend:gateway:bootRun
JAVA_HOME=/path/to/jdk17 ./gradlew :backend:ingestion:bootRun
JAVA_HOME=/path/to/jdk17 ./gradlew :backend:stream-processor:bootRun
JAVA_HOME=/path/to/jdk17 ./gradlew :backend:geospatial:bootRun
```

**Frontend:**

```bash
cd frontend/digital-twin-earth-app
npm install
npm start   # http://localhost:4200, proxies /api → localhost:8080
```

**Build all backend JARs:**

```bash
JAVA_HOME=/path/to/jdk17 ./gradlew build -x test
```

## Configuration

All sensitive configuration is environment-variable driven. No keys are committed.

| Variable | Default | Description |
|---|---|---|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/digitaltwin` | PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `dt_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `dt_pass` | DB password |
| `SPRING_REDIS_HOST` | `localhost` | Redis host |
| `GATEWAY_API_KEY` | `dev-key` | API authentication key |
| `GATEWAY_GEOSPATIAL_URL` | `http://localhost:8083` | Internal geospatial URL |
| `GATEWAY_CORS_ORIGINS` | `http://localhost:4200` | Allowed CORS origins |
| `INGESTION_FEED_FLIGHT` | `mock` | Feed type (`mock` or future adapters) |
| `INGESTION_SIM_AIRCRAFT` | `500` | Simulated aircraft count |
| `INGESTION_SIM_VESSELS` | `200` | Simulated vessel count |
| `INGESTION_SIM_INTERVAL_MS` | `1000` | Ingestion tick interval (ms) |

## Repository Structure

```
/
  LICENSE
  README.md
  AGENTS.MD               ← full project specification
  settings.gradle.kts
  build.gradle.kts
  gradle/
    libs.versions.toml    ← single version catalog
    wrapper/

  backend/
    shared/               ← shared domain, DTOs, events, constants
    gateway/              ← public API + SSE fan-out  :8080
    ingestion/            ← mock data generator + Kafka producer  :8081
    stream-processor/     ← Kafka consumer + DB writer  :8082
    geospatial/           ← PostGIS + Redis query service  :8083

  frontend/
    digital-twin-earth-app/   ← Angular 17 SPA

  deploy/
    docker-compose.yml

  docs/
    ARCHITECTURE.md       ← system design and data flow
    API.md                ← REST + SSE endpoint reference
    ZING.md               ← Azul Zing JVM tuning guide
```

## API Summary

| Endpoint | Description |
|---|---|
| `GET /api/flights?bbox=...` | Current flight positions in bounding box |
| `GET /api/flights/{id}/track?from=&to=` | Historical track for one aircraft |
| `GET /api/ships?bbox=...` | Current vessel positions |
| `GET /api/ships/{id}/track?from=&to=` | Historical track for one vessel |
| `POST /api/scene/query` | Combined scene query (flights + ships) |
| `GET /api/stream/flights` | SSE stream of live flight updates |
| `GET /api/stream/ships` | SSE stream of live ship updates |

See [docs/API.md](docs/API.md) for full reference.

## Zing Stress Testing

See [docs/ZING.md](docs/ZING.md) for:

- JVM flag examples for large heaps (32–256 GB)
- Increasing ingestion rate (up to 100k msgs/sec)
- k6 load test scripts for REST and SSE endpoints
- GC log analysis with Zing C4

To run at high ingestion rate:

```bash
java \
  -Xmx32g -Xms32g \
  -XX:+AlwaysPreTouch \
  -Dingestion.simulation.aircraft-count=2000 \
  -Dingestion.simulation.interval-ms=5 \
  -jar backend/ingestion/build/libs/ingestion.jar
```

## License

MIT — see [LICENSE](LICENSE).

Dependencies are Apache 2.0, MIT, or BSD licensed. No GPL or AGPL dependencies.
