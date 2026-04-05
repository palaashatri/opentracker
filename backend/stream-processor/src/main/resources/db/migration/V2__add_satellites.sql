CREATE TABLE IF NOT EXISTS satellites (
    id UUID PRIMARY KEY,
    norad_id VARCHAR(10) NOT NULL,
    name VARCHAR(100),
    source VARCHAR(30) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_satellites_norad_id ON satellites(norad_id);

CREATE TABLE IF NOT EXISTS satellite_positions (
    id UUID NOT NULL,
    satellite_id UUID NOT NULL REFERENCES satellites(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    altitude_km DOUBLE PRECISION NOT NULL,
    velocity_km_s DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS idx_sat_positions_time ON satellite_positions(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_sat_positions_satellite_time ON satellite_positions(satellite_id, recorded_at DESC);
