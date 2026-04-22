# Technical Design Document: Aetheris Platform

**Product Name:** Aetheris
**Tagline:** *The definitive geospatial intelligence operating system.*

---

## 1. Overview
Aetheris is a production-grade, full-stack geospatial intelligence (GEOINT) platform designed to provide a real-time, 3D "God's-Eye" view of Earth. It ingests, normalizes, and visualizes live Open-Source Intelligence (OSINT) telemetry—including ADS-B (aviation), AIS (maritime), and TLE (satellite orbital data). 

The system is engineered with two primary mandates:
1. **Frontend:** Deliver a heavily optimized, WebGPU-accelerated 3D client that perfectly mimics a native macOS application via browser APIs.
2. **Backend:** Serve as a highly concurrent, low-latency JVM streaming architecture designed to stress-test garbage collection (GC), heap management, and I/O under massive scale.

---

## 2. Architecture 

The architecture is built around a decoupled, event-driven pipeline capable of handling millions of spatial updates per minute.

### 2.1 Service Boundaries
* **Ingestion Gateways:** Lightweight, protocol-specific workers (TCP sockets for ADS-B, UDP for AIS, HTTP chron jobs for TLE).
* **Stream Processing Engine (The JVM Core):** The primary backend service responsible for spatial indexing, state management, and client broadcasting.
* **API & Query Service:** Handles static metadata lookups, historical data queries, and client initialization.

### 2.2 API & State Synchronization
* **Query Layer:** **GraphQL**. 
    * *Justification:* Client views require vastly different data shapes. A zoomed-out view needs only `[id, lat, lon, type]`, while a selected entity requires deep relational data (e.g., `[airline, origin, destination, aircraft_type, velocity]`). GraphQL prevents massive over-fetching of telemetry metadata.
* **State Synchronization:** **WebSockets (Binary/Protobuf over WS)**.
    * *Justification over SSE:* Server-Sent Events are unidirectional. Aetheris requires *bi-directional* communication for dynamic spatial filtering. As the user pans the 3D camera, the client continually sends its viewing frustum (bounding box) to the server via WS. The server instantly dynamically filters the downstream telemetry, pushing updates only for entities within the active viewport.

### 2.3 Scaling Strategy
* **Horizontal:** The JVM streaming core is horizontally scaled using an actor model or consistent hashing based on spatial regions (e.g., Uber's H3 hexagon grid). Each JVM node manages a subset of the globe's state.
* **Vertical:** Nodes are provisioned with massive RAM (128GB+) to maintain an in-memory spatial grid of the world, specifically designed to test JVM Large Heap capabilities.

---

## 3. Data Model

All real-time entities inherit from a base spatial schema, heavily optimized for serialization (Protocol Buffers).

```protobuf
message GeoEntity {
  string id = 1;                  // Unique identifier (ICAO, MMSI, NORAD ID)
  EntityType type = 2;            // FLIGHT, SHIP, SATELLITE
  double latitude = 3;
  double longitude = 4;
  float altitude = 5;             // In meters
  float velocity = 6;             // In knots or km/s
  float heading = 7;              // 0-360 degrees
  int64 timestamp = 8;            // Epoch ms
  bytes compressed_metadata = 9;  // Lazy-loaded metadata specific to type
}
```

---

## 4. Ingestion & Streaming Layer

### 4.1 Data Sources & Frequency
1.  **ADS-B (Flights):** Streams via aggregators (e.g., OpenSky Network or raw SDR TCP feeds). Updates arrive at **~1-10 Hz** per aircraft.
2.  **AIS (Ships):** Streams via NMEA payloads. Updates are slower, ranging from **every 2 seconds to 3 minutes** depending on ship speed and class.
3.  **TLE (Satellites):** Two-Line Element sets pulled via HTTP from CelesTrak/Space-Track **every 12-24 hours**. 

### 4.2 Processing Pipeline
1.  **Normalization:** Raw bytes are parsed into the unified `GeoEntity` Protobuf model.
2.  **Spatial Indexing:** Entities are inserted into a highly concurrent in-memory spatial index (e.g., a concurrent QuadTree or H3 index) residing in the JVM heap.
3.  **Propagation:** * *For ADS-B/AIS:* Direct passthrough to the broadcast layer.
    * *For Satellites:* TLEs are sent to the client once. The *client* uses the SGP4 algorithm in WebAssembly/JS to calculate real-time orbital positions, saving backend bandwidth.

---

## 5. Testing & JVM Stress Strategy

The backend is explicitly designed to act as a torture test for modern JVMs (Java 21+, utilizing Virtual Threads and advanced GCs like ZGC or Shenandoah).

### 5.1 Load Patterns
* **Heap Pressure (Long-Lived vs. Short-Lived):** The spatial index holds millions of entity states (long-lived, promoting to Old Gen). Meanwhile, the ingestion layer parses hundreds of thousands of incoming updates per second, creating massive amounts of short-lived garbage (Young Gen). This tests the GC's ability to handle high allocation rates without fragmenting the massive heap.
* **High Concurrency:** We simulate 100,000+ concurrent WebSocket clients. Using Java 21 **Virtual Threads (Project Loom)**, each WebSocket connection maps to a lightweight virtual thread, testing the runtime's scheduler and network I/O limits.
* **Synthetic Traffic Generation:** A dedicated load-testing harness (written in Kotlin + Gatling) injects synthetic NMEA and ADS-B traffic into the TCP boundaries, scaling up to 5M messages/second to measure GC pause times (targeting <1ms max pause with ZGC).

---

## 6. Rendering Pipeline (WebGPU/WebGL2)

The 3D canvas relies on a custom engine (or highly customized deck.gl/Cesium build) targeting WebGPU with WebGL2 fallback.

* **Globe & Tile Loading:** Vector tiles and satellite imagery are loaded via a quadtree Level of Detail (LoD) system. Only tiles within the camera's view are requested.
* **Instanced Rendering:** To render 50,000+ flights and ships simultaneously at 60FPS, Aetheris uses **Instanced Meshes**. A single 3D model of a plane/ship is uploaded to the GPU once. The GPU buffers are updated with an array of transformations (position, rotation, scale) for every instance.
* **Frustum Culling:** Executed on a Web Worker. Entities outside the camera's viewport are dropped from the GPU buffer updates.
* **Time-Based Animation:** Server updates arrive every few seconds, but the client renders at 60Hz. The client uses **Hermite spline interpolation** between the last two known coordinates to smoothly animate movement, eliminating stutter.

---

## 7. UI/UX: Native macOS Language

When running fullscreen (`F11` or PWA install) on Safari or Chrome, Aetheris explicitly mimics Apple's Human Interface Guidelines (HIG).

### 7.1 Design Primitives
* **Materials:** Extensive use of CSS `backdrop-filter: blur(30px) saturate(150%)` to create standard macOS vibrancy and translucency for panels overlapping the 3D globe.
* **Typography:** CSS `font-family: system-ui, -apple-system, BlinkMacSystemFont, "SF Pro", sans-serif`.
* **Iconography:** Direct mapping of Apple's **SF Symbols** (e.g., `airplane`, `ferry.fill`, `globe.americas.fill`).

### 7.2 Screen Layouts
1.  **Main Globe View (The Canvas):** The edge-to-edge 3D WebGPU canvas. Smooth, momentum-based panning and zooming.
2.  **Translucent Sidebar (Left):**
    * Mimics the macOS Finder sidebar.
    * **Sections:** Favorites, Layers (Flights, Maritime, Space, Weather, Geopolitical Boundaries).
    * **Controls:** macOS-style toggle switches and segmented controls to filter traffic.
3.  **Spotlight Search (Floating Center-Top):**
    * Triggered via `Cmd+K`.
    * A translucent, floating command palette to instantly search for a specific flight number, ship name, or coordinate.
4.  **Inspector Panel (Right):**
    * Slides in when an entity is clicked.
    * Displays high-resolution imagery of the vessel/aircraft, live telemetry gauges (altitude, speed, heading), and flight/route path history drawn as glowing neon trajectories on the globe.

---

## 8. Licensing & Project Structure

### 8.1 Licensing
The project is open-source under the **MIT License**.

**Sample `LICENSE` text:**
> MIT License
>
> Copyright (c) 2026 Aetheris Contributors
>
> Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software...

### 8.2 Repository Layout
A monorepo structure managed via Gradle (Backend) and pnpm (Frontend).

```text
aetheris/
├── .github/workflows/       # CI/CD Actions
├── backend/                 # JVM Backend 
│   ├── ingestion/           # Netty/TCP ingestion services
│   ├── streaming-core/      # Spatial index, Websockets, GC tuning configs
│   ├── graphql-api/         # Schema and resolvers
│   └── load-generator/      # Gatling stress tests
├── frontend/                # Web Application
│   ├── src/
│   │   ├── components/      # React/Solid components (macOS UI)
│   │   ├── engine/          # WebGPU/WebGL2 rendering pipeline
│   │   ├── network/         # WS/Protobuf client
│   │   └── workers/         # SGP4 / Frustum culling Web Workers
│   └── package.json
├── infra/                   # Infrastructure as Code
│   ├── k8s/                 # Kubernetes manifests / Helm charts
│   ├── docker/              # Dockerfiles (JVM base, Node base)
│   └── terraform/           # Cloud provisioning
├── docs/                    # Architecture diagrams and API docs
└── LICENSE                  # MIT License file
```

### 8.3 Build and Deployment Pipeline (CI/CD)
1.  **Commit/PR:** Triggers GitHub Actions.
2.  **Backend CI:** Compiles Java/Kotlin, runs JUnit tests, and executes a 5-minute JMH microbenchmark to detect GC regression.
3.  **Frontend CI:** Runs Vite build, transpiles TypeScript, runs WebGL headless tests, and builds static assets.
4.  **Containerization:** Builds multi-arch Docker images (linux/amd64, linux/arm64) and pushes to GitHub Container Registry (GHCR).
5.  **Deployment (CD):** ArgoCD detects registry updates and applies rolling updates to the Kubernetes cluster, gracefully draining long-lived WebSocket connections to prevent client disruption.
