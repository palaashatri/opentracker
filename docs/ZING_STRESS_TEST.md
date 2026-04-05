# ZING_STRESS_TEST.md
Using Digital Twin Earth to stress-test Azul Zing

## 1. Purpose

This document explains how the Digital Twin Earth application exercises Azul Zing (Azul Platform Prime) in a realistic, high-stress scenario:

- High-rate streaming ingestion (ADS-B, AIS, satellites).
- Geospatial + time-series queries.
- WebSocket/SSE fan-out to many clients.
- Large heap, high allocation churn, long uptimes.

It is intended as a technical brief you can share with managers and JVM engineers.

---

## 2. Why this app is a good Zing workload

**Workload characteristics:**

- Continuous, unbounded streams of data (flights, ships, satellites).
- High object allocation rate in ingestion and processing pipelines.
- Medium-lived objects in caches and query results.
- Spiky load patterns (time-scrubbing, replay, zooming).
- Many concurrent clients consuming live updates.

These are exactly the scenarios where:

- Traditional stop-the-world GC can cause visible pauses.
- Zing's C4 collector and large-heap capabilities shine.

---

## 3. Zing run configuration (example)

```bash
JAVA_OPTS="
  -Xms64g
  -Xmx64g
  -XX:+UseC4
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseNUMA
  -XX:+AlwaysPreTouch
  -XX:+PrintGC
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
  -Xloggc:/var/log/digital-twin-earth-gc.log
"
```

Run key backend services (gateway, ingestion, stream-processor, geospatial) on Zing with a large heap (e.g. 32–128 GB).

---

## 4. Stress scenarios

### 4.1 Ingestion stress

**Goal:** Maximize allocation rate and sustained throughput.

Enable mock mode with high counts:

```yaml
mock:
  enabled: true
  seed: 42
  flights:
    count: 20000
  ships:
    count: 10000
  satellites:
    count: 5000
```

Tick intervals:

- Flights: 10–20 updates/sec (`ingestion.simulation.interval-ms=50`)
- Ships: 2–5 updates/sec (`ingestion.simulation.interval-ms=200`)
- Satellites: 1 update/sec

**Effect on Zing:**

- High, continuous allocation in ingestion services.
- Large volumes of short-lived objects (records, Jackson nodes, Kafka ProducerRecord).
- C4 should maintain low, stable pause times throughout.

---

### 4.2 Query and replay stress

**Goal:** Stress geospatial queries, JSON serialization, and medium-lived objects.

- Preload several hours of history (mock or real).
- Run load tests against:

  ```bash
  GET /api/flights?minLat=-90&maxLat=90&minLon=-180&maxLon=180
  GET /api/flights/{id}/track?from=...&to=...
  POST /api/scene/query
  ```

- Simulate time-scrubbing: clients repeatedly request tracks for sliding windows at high frequency.

**Effect on Zing:**

- Large result sets → many medium-lived objects.
- High JSON (de)serialization churn.
- C4 should avoid long pauses even under heavy query load.

---

### 4.3 Streaming fan-out stress

**Goal:** Stress SSE handling and tail latency.

Start 1,000–10,000 simulated clients subscribing to:

```
GET /api/stream/flights
GET /api/stream/ships
GET /api/stream/satellites
```

Each client maintains a long-lived connection and receives frequent updates.

**Effect on Zing:**

- Many live objects (SseEmitter instances, String buffers).
- Continuous small allocations per broadcast.
- Tail latency is sensitive to GC pauses — Zing should keep it flat.

---

## 5. Metrics to observe

Use Grafana/Prometheus or equivalent to track:

- **GC metrics:** pause times (max, p95, p99), allocation rate, live set size.
- **Application metrics:** ingestion throughput (msgs/sec), query latency (p50/p95/p99), stream update latency (producer → client).
- **System metrics:** CPU utilization, memory usage, network throughput.

The expectation for Zing:

- Near-zero observable GC pauses.
- Stable latency even as heap and load grow.
- Predictable behavior under spikes (time-scrub storms, burst connections).

---

## 6. Suggested experiment plan

1. **Baseline (small heap, low load):** 4–8 GB heap, modest mock counts. Verify correctness.
2. **Scale up heap:** 32–64 GB, increase mock entities by 10×. Observe GC.
3. **Spike tests:** sudden ingestion rate increase, burst query/replay traffic, many new streaming clients.
4. **Long-running soak:** 24–72 hours. Monitor memory fragmentation, GC stability, latency drift.

---

## 7. Talking points for managers

- This is not a synthetic micro-benchmark; it's a **realistic, end-to-end system**: multiple services, streaming + storage + queries + UI.
- It mimics real customer workloads: high-volume telemetry, geospatial analytics, live dashboards.
- Zing's value is visible in: smooth user experience (no UI freezes due to backend pauses), stable tail latency under heavy load, ability to run with very large heaps without GC tuning gymnastics.

This makes Digital Twin Earth a strong **demo and evaluation platform** for Azul Zing.
