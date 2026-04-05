  # Running Digital Twin Earth on Azul Zing (Platform Prime)

This document covers how to configure and stress-test the Digital Twin Earth application on Azul Zing JVM.

## Why Zing?

Azul Zing (Azul Platform Prime) uses the C4 garbage collector, which provides:
- Sub-millisecond GC pause times at large heap sizes (32–256 GB)
- Pause-less compaction via GPGC (Generalized Pauseless GC)
- ReadyNow! on-stack replacement for fast warm-up

Digital Twin Earth generates high object churn in:
- Kafka consumer deserialization (AircraftRawEvent, ShipRawEvent records)
- Jackson JSON serialization (AircraftPositionDto, VesselPositionDto)
- SSE fan-out (String allocation per broadcast)
- Redis pipeline operations

This makes it an excellent Zing benchmark workload.

---

## Replacing the JVM

Swap the Docker base images in each service's `Dockerfile`:

```dockerfile
# Standard (default)
FROM eclipse-temurin:17

# Replace with Azul Zing (requires Azul Docker registry access)
FROM azul/zulu-prime:17-jre
```

Or on bare metal, point `JAVA_HOME` at your Zing installation:

```bash
export JAVA_HOME=/opt/azul/zulu-prime-17
export PATH=$JAVA_HOME/bin:$PATH
java -version  # should print Azul Platform Prime
```

No application code changes are needed. The codebase uses no JVM-vendor APIs.

---

## JVM Flags for Stream Processor (High-Throughput Service)

The stream-processor is the most GC-sensitive service — it ingests Kafka messages, creates entities, and writes to both Redis and PostgreSQL.

```bash
java \
  -Xmx32g \
  -Xms32g \
  -XX:+AlwaysPreTouch \
  -Xlog:gc*:file=/var/log/dt/gc-stream-processor.log:time,uptime:filecount=5,filesize=20m \
  -Dspring.profiles.active=docker \
  -jar stream-processor.jar
```

For larger heaps (256 GB on high-memory hosts):

```bash
java \
  -Xmx256g \
  -Xms256g \
  -XX:+AlwaysPreTouch \
  -Xlog:gc*:file=/var/log/dt/gc-stream-processor.log:time,uptime:filecount=10,filesize=50m \
  -jar stream-processor.jar
```

**Note:** Zing's C4 GC does not require `-XX:+UseZGC` or `-XX:+UseG1GC` flags. The Zing JVM uses C4 by default. Do not pass GC selection flags — they may be ignored or cause an error.

---

## JVM Flags for Gateway (High-Concurrency Service)

The gateway holds many `SseEmitter` connections concurrently. Tune Tomcat thread pool via `application.yml` (already configured at 400 threads max), and size the heap for connection state:

```bash
java \
  -Xmx8g \
  -Xms8g \
  -XX:+AlwaysPreTouch \
  -Xlog:gc*:file=/var/log/dt/gc-gateway.log:time,uptime \
  -jar gateway.jar
```

---

## JVM Flags for Ingestion (High-Allocation Service)

The ingestion service creates 500+ AircraftRawEvent records per tick at configurable rates. Set a moderate heap:

```bash
java \
  -Xmx4g \
  -Xms4g \
  -XX:+AlwaysPreTouch \
  -Dingestion.simulation.aircraft-count=5000 \
  -Dingestion.simulation.interval-ms=100 \
  -jar ingestion.jar
```

---

## ReadyNow! Warm-Up

Zing's ReadyNow! feature profiles the application during warm-up and re-uses the profile on subsequent starts for faster peak performance.

**Warm-up probe:** After starting any service, hit the actuator health endpoint:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Run a brief load burst (30–60 seconds) after startup to allow ReadyNow! to capture hot methods before saving the profile. Point `JAVA_HOME/etc/readynow/` at a persistent directory to reuse profiles across restarts.

---

## Increasing Ingestion Rate

To stress-test at 10k–100k messages/second:

```bash
# 10k msgs/sec: 500 aircraft, 20ms interval
java -Dingestion.simulation.aircraft-count=500 \
     -Dingestion.simulation.interval-ms=20 \
     -jar ingestion.jar

# 50k msgs/sec: 1000 aircraft, 10ms interval, batch size 50
java -Dingestion.simulation.aircraft-count=1000 \
     -Dingestion.simulation.interval-ms=10 \
     -Dingestion.simulation.batch-size=50 \
     -jar ingestion.jar

# 100k msgs/sec: 2000 aircraft, 5ms interval, batch size 100
java -Dingestion.simulation.aircraft-count=2000 \
     -Dingestion.simulation.interval-ms=5 \
     -Dingestion.simulation.batch-size=100 \
     -jar ingestion.jar
```

**Kafka producer tuning** for high throughput (set in `application.yml` or env):

```yaml
spring:
  kafka:
    producer:
      batch-size: 65536       # 64 KB batch
      buffer-memory: 33554432 # 32 MB buffer
      linger-ms: 5            # wait 5ms to batch
      compression-type: lz4
```

---

## Load Testing REST Endpoints

Use [k6](https://k6.io) or [Gatling](https://gatling.io) against the gateway.

### k6 — Current Flights Snapshot

```javascript
// load-test-flights.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 200,
  duration: '5m',
};

export default function () {
  const res = http.get('http://localhost:8080/api/flights?minLat=-90&maxLat=90&minLon=-180&maxLon=180');
  check(res, { 'status 200': r => r.status === 200 });
  sleep(0.1);
}
```

```bash
k6 run load-test-flights.js
```

### k6 — SSE Connection Sustain

```javascript
// load-test-sse.js
import http from 'k6/http';

export const options = { vus: 500, duration: '10m' };

export default function () {
  // k6 doesn't natively support SSE; use HTTP GET and read partial body
  const res = http.get('http://localhost:8080/api/stream/flights', {
    headers: { 'Accept': 'text/event-stream' },
    timeout: '60s'
  });
}
```

---

## Observing GC Behavior

With GC logging enabled, parse the log to observe:

```bash
# Count GC events and average pause
grep "GC(" /var/log/dt/gc-stream-processor.log | wc -l

# Show pause times (Zing C4 typically shows 0–1ms pauses)
grep "Pause" /var/log/dt/gc-stream-processor.log | awk '{print $NF}'
```

On Zing, you should observe:
- Zero stop-the-world pauses (C4 is fully concurrent)
- Allocation rate visible via `-Xlog:gc+alloc`
- Promotion rate near zero (short-lived records are collected in-place)

---

## Baseline Comparison

To compare Zing vs. standard OpenJDK G1:

```bash
# OpenJDK G1 baseline
JAVA_HOME=/path/to/openjdk17 java \
  -XX:+UseG1GC \
  -Xmx32g -Xms32g \
  -Xlog:gc*:file=gc-g1.log \
  -jar stream-processor.jar

# Zing C4
JAVA_HOME=/opt/azul/zulu-prime-17 java \
  -Xmx32g -Xms32g \
  -Xlog:gc*:file=gc-zing.log \
  -jar stream-processor.jar
```

Compare `gc-g1.log` vs `gc-zing.log` for pause frequency and duration under the same ingestion load.
