# API Reference — Digital Twin Earth

Base URL: `http://localhost:8080`

All endpoints require header `X-Api-Key: <key>` (default dev key: `dev-key`, or pass `?api_key=dev-key`).

---

## Flights

### GET /api/flights

Returns current positions for all aircraft within a bounding box.

**Query parameters:**

| Param | Type | Default | Description |
|---|---|---|---|
| `minLat` | float | -90 | South boundary |
| `maxLat` | float | 90 | North boundary |
| `minLon` | float | -180 | West boundary |
| `maxLon` | float | 180 | East boundary |

**Response:** `200 OK`, `application/json`

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "aircraftId": "550e8400-e29b-41d4-a716-446655440000",
    "icao24": "aa1234",
    "callsign": "AAL123",
    "timestamp": "2024-01-15T14:30:00Z",
    "lat": 40.6413,
    "lon": -73.7781,
    "altitudeMeters": 10668.0,
    "groundSpeedMps": 245.0,
    "headingDeg": 87.5,
    "verticalRateMps": 0.0
  }
]
```

### GET /api/flights/{id}/track

Returns historical position track for an aircraft.

**Path parameters:** `id` — aircraft UUID

**Query parameters:**

| Param | Type | Required | Description |
|---|---|---|---|
| `from` | ISO-8601 | Yes | Start of time range |
| `to` | ISO-8601 | Yes | End of time range |

**Response:** `200 OK`

```json
{
  "entityId": "550e8400-e29b-41d4-a716-446655440000",
  "entityType": "aircraft",
  "points": [
    {
      "timestamp": "2024-01-15T14:00:00Z",
      "lat": 40.0,
      "lon": -74.5,
      "altitudeMeters": 10500.0,
      "headingDeg": 85.0
    }
  ]
}
```

---

## Ships

### GET /api/ships

Returns current positions for all vessels within a bounding box. Same bbox parameters as `/api/flights`.

**Response:**

```json
[
  {
    "id": "vessel-uuid",
    "vesselId": "vessel-uuid",
    "mmsi": "366123456",
    "name": "EVER GIVEN",
    "timestamp": "2024-01-15T14:30:00Z",
    "lat": 31.2556,
    "lon": 32.3111,
    "speedKnots": 8.5,
    "courseDeg": 143.0
  }
]
```

### GET /api/ships/{id}/track

Same as `/api/flights/{id}/track` but for vessels. Returns `entityType: "vessel"`.

---

## Scene

### POST /api/scene/query

Query flights and ships together in a single request.

**Request body:** `application/json`

```json
{
  "minLat": 30.0,
  "maxLat": 60.0,
  "minLon": -30.0,
  "maxLon": 30.0,
  "from": "2024-01-15T14:00:00Z",
  "to": "2024-01-15T14:30:00Z",
  "layers": ["flights", "ships"]
}
```

**Response:**

```json
{
  "flights": [ /* AircraftPositionDto[] */ ],
  "ships":   [ /* VesselPositionDto[]  */ ],
  "queriedAt": "2024-01-15T14:30:01Z"
}
```

Omit `layers` (or send empty array) to query both. Set `from`/`to` to the same instant for a snapshot.

---

## Streaming (SSE)

### GET /api/stream/flights

Opens a Server-Sent Events stream of live aircraft position updates.

**Headers:** `Accept: text/event-stream`

**Event format:**

```
data: {"id":"...","icao24":"aa1234","callsign":"AAL123","lat":40.64,"lon":-73.77,...}

```

Each event is a complete `AircraftPositionDto` JSON object. Events arrive at the configured ingestion rate (default ~1 event per aircraft per second).

### GET /api/stream/ships

Same as `/api/stream/flights` but emits `VesselPositionDto` objects.

---

## Management

### GET /actuator/health

```json
{ "status": "UP" }
```

### GET /actuator/metrics

Standard Spring Boot Micrometer metrics (JVM, HTTP, Kafka consumer lag, etc.).

---

## Error Responses

| Status | Condition |
|---|---|
| 401 | Missing or invalid `X-Api-Key` |
| 429 | Rate limit exceeded (200 req/min per IP) |
| 502 | Geospatial service unavailable |
| 504 | Upstream timeout |

```json
{ "error": "Unauthorized" }
```
