# Architecture — Digital Twin Earth

## Overview

Digital Twin Earth is a microservices application that renders live and historical flight and ship tracking data on a 3D globe. It is structured to run under high load on the Azul Zing JVM.

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                              │
│  Angular 17 + CesiumJS  ←── SSE ──┐                        │
│  (http://localhost:4200)  REST ────┼──────────────────────┐ │
└────────────────────────────────────┼──────────────────────┘ │
                                     │                        │
                        ┌────────────▼──────────┐            │
                        │   Gateway  :8080       │            │
                        │  (Spring Boot MVC)     │            │
                        │  REST proxy + SSE fan  │            │
                        └──────┬────────┬────────┘            │
                               │        │ Kafka consumer       │
                     REST      │        │ (flights/ships.proc) │
                               │        │                      │
              ┌────────────────▼──┐   ┌─▼──────────────────┐  │
              │  Geospatial :8083 │   │  Kafka              │  │
              │  (Spring Boot)    │   │  flights.raw        │  │
              │  PostGIS queries  │   │  ships.raw          │  │
              │  Redis cache read │   │  flights.processed  │  │
              └────────┬──────────┘   │  ships.processed    │  │
                       │              └──────────┬───────────┘  │
              ┌────────▼──────────────────────────▼──────────┐  │
              │           PostgreSQL + PostGIS                │  │
              │           + Redis cache                       │  │
              └───────────────────────────────────────────────┘  │
                                      ▲                          │
                        ┌─────────────┴────────┐                 │
                        │  Stream Processor    │                 │
                        │  :8082               │                 │
                        │  Kafka consumer      │                 │
                        │  Dedup + persist     │                 │
                        └──────────────────────┘                 │
                                      ▲                          │
                        ┌─────────────┴────────┐                 │
                        │  Ingestion  :8081     │                 │
                        │  Mock simulators or   │                 │
                        │  real ADS-B / AIS     │                 │
                        └──────────────────────┘                 │
```

## Services

| Service | Port | Role |
|---|---|---|
| `gateway` | 8080 | Public API, CORS, auth, SSE fan-out |
| `ingestion` | 8081 | Data feed → Kafka producer |
| `stream-processor` | 8082 | Kafka consumer → PostgreSQL + Redis |
| `geospatial` | 8083 | Internal REST over PostGIS / Redis |
| PostgreSQL | 5432 | Persistent geospatial + time-series store |
| Redis | 6379 | Current-position cache (TTL 60s) |
| Kafka | 9092 | Message bus |
| Frontend | 4200 (dev) / 80 (prod) | Angular SPA |

## Data Flow

```
1. Ingestion ticks every N ms
   └─ MockFlightFeedProvider.fetch() → AircraftRawEvent[]
   └─ FlightEventPublisher → Kafka: flights.raw

2. Stream Processor consumes flights.raw
   └─ FlightRawConsumer → FlightProcessor
   └─ Find-or-create AircraftEntity (by icao24)
   └─ Save AircraftPositionEntity
   └─ Write Redis: flight:current:{icao24} TTL=60s
   └─ Publish AircraftProcessedEvent → Kafka: flights.processed

3. Gateway SSE fan-out
   └─ FlightSseService @KafkaListener flights.processed
   └─ SseBroadcaster → all registered SseEmitter clients

4. Browser EventSource
   └─ SSE stream → FlightService.streamFlights()
   └─ GlobeEngineService.addOrUpdateFlight()
   └─ CesiumJS entity updated in real time

5. REST snapshot (on load / bbox change)
   └─ Gateway GET /api/flights?bbox=...
   └─ GeospatialClient → geospatial GET /internal/flights
   └─ FlightQueryService: Redis scan → bbox filter
   └─ Fallback: PostgreSQL native SQL
```

## Module Map

```
backend/
  shared/          com.digitaltwin.shared
    domain/        Aircraft, AircraftPosition, Vessel, VesselPosition (records)
    dto/           *Dto records — JSON transfer objects
    event/         *RawEvent, *ProcessedEvent — Kafka message records
    constants/     KafkaTopics, ApiPaths

  gateway/         com.digitaltwin.gateway
    config/        WebConfig (CORS, RestClient), GatewayProperties, JacksonConfig
    filter/        ApiKeyFilter, RateLimitFilter
    proxy/         GeospatialClient (RestClient)
    sse/           SseBroadcaster, FlightSseService, ShipSseService
    controller/    FlightController, ShipController, SceneController, StreamController

  ingestion/       com.digitaltwin.ingestion
    feed/          FlightFeedProvider, ShipFeedProvider (interfaces)
    feed/mock/     FlightSimulator, ShipSimulator, Mock*Provider
    publisher/     FlightEventPublisher, ShipEventPublisher
    scheduler/     IngestionScheduler (@Scheduled)

  stream-processor/ com.digitaltwin.processor
    consumer/      FlightRawConsumer, ShipRawConsumer (@KafkaListener)
    processing/    FlightProcessor, ShipProcessor
    entity/        AircraftEntity, AircraftPositionEntity, VesselEntity, VesselPositionEntity
    repository/    Spring Data JPA repositories
    cache/         PositionCacheService (Redis)

  geospatial/      com.digitaltwin.geospatial
    repository/    AircraftPositionQueryRepository, VesselPositionQueryRepository (EntityManager)
    service/       FlightQueryService, ShipQueryService, SceneQueryService
    controller/    FlightQueryController, ShipQueryController, SceneController

frontend/
  digital-twin-earth-app/   Angular 17 standalone
    models/        aircraft.model.ts, vessel.model.ts, scene.model.ts
    services/      GlobeEngineService, FlightService, ShipService, SseService, TimeControlService
    globe/         GlobeComponent + LayerToggle, Timeline, StatsPanel, EntityDetail
```

## Key Design Decisions

### No Spring Cloud Gateway
Using plain Spring MVC for the gateway. Spring Cloud Gateway is reactive (Netty + Reactor). For Zing stress-testing the important workload is ingestion and stream processing — a simpler synchronous gateway is easier to profile and avoids reactor scheduler complexity.

### Records for all DTOs and events
Java records (stable in 17) are immutable, zero-boilerplate, and serialise cleanly with Jackson + `ParameterNamesModule`. All Kafka events and REST DTOs are records.

### No Hibernate Spatial geometry types
Entities store raw `double` lat/lon. Bounding-box queries use SQL `BETWEEN`. This avoids the Hibernate Spatial / PostGIS dialect complexity while keeping the DB schema PostGIS-ready for advanced spatial queries via native SQL.

### Redis TTL pattern
Current positions are written to Redis with a 60-second TTL by the stream-processor. The geospatial service scans `flight:current:*` and `ship:current:*` keys for live queries. Stale entities expire automatically.

### SSE over WebSocket (first pass)
SSE is simpler (HTTP/1.1, no upgrade handshake, auto-reconnect in browsers). The `SseBroadcaster` uses `CopyOnWriteArrayList<SseEmitter>` for thread safety. Dead emitters are removed on error/timeout/completion.

### Zing JVM Compatibility
- No virtual threads (Java 21 feature, not guaranteed on Zing)
- No preview/experimental features (`--release 17` enforced)
- No Reactor/Netty (threading model differs from platform threads)
- All GC-sensitive paths use standard Java collections

## Database Schema

```sql
aircraft           (id UUID PK, icao24, callsign, airline, model, country, source)
vessels            (id UUID PK, mmsi, imo, name, type, flag, source)
aircraft_positions (id UUID, aircraft_id FK, recorded_at TIMESTAMPTZ, lat, lon,
                    altitude_m, ground_speed_mps, heading_deg, vertical_rate_mps)
vessel_positions   (id UUID, vessel_id FK, recorded_at TIMESTAMPTZ, lat, lon,
                    speed_knots, course_deg)
```

Indexes:
- `aircraft_positions(recorded_at DESC)` — recent data queries
- `aircraft_positions(aircraft_id, recorded_at DESC)` — track queries
- `vessel_positions` — same pattern

Flyway manages all migrations from `backend/stream-processor/src/main/resources/db/migration/`.
