CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS aircraft (
    id UUID PRIMARY KEY,
    icao24 VARCHAR(10) NOT NULL,
    callsign VARCHAR(20),
    airline VARCHAR(50),
    model VARCHAR(50),
    country VARCHAR(50),
    source VARCHAR(30) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_aircraft_icao24 ON aircraft(icao24);

CREATE TABLE IF NOT EXISTS vessels (
    id UUID PRIMARY KEY,
    mmsi VARCHAR(15) NOT NULL,
    imo VARCHAR(15),
    name VARCHAR(100),
    type VARCHAR(30),
    flag VARCHAR(10),
    source VARCHAR(30) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_vessels_mmsi ON vessels(mmsi);

CREATE TABLE IF NOT EXISTS aircraft_positions (
    id UUID NOT NULL,
    aircraft_id UUID NOT NULL REFERENCES aircraft(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    altitude_m DOUBLE PRECISION,
    ground_speed_mps DOUBLE PRECISION,
    heading_deg DOUBLE PRECISION,
    vertical_rate_mps DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_aircraft_positions_time ON aircraft_positions(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_aircraft_positions_aircraft_time ON aircraft_positions(aircraft_id, recorded_at DESC);

CREATE TABLE IF NOT EXISTS vessel_positions (
    id UUID NOT NULL,
    vessel_id UUID NOT NULL REFERENCES vessels(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    speed_knots DOUBLE PRECISION,
    course_deg DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_vessel_positions_time ON vessel_positions(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_vessel_positions_vessel_time ON vessel_positions(vessel_id, recorded_at DESC);
