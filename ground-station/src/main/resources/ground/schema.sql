-- The archive of the ground station: one row per mission run, its telemetry and its events.

CREATE TABLE missions (
    mission_id  TEXT PRIMARY KEY,
    file        TEXT,
    planet      TEXT,
    seed        BIGINT,
    started_at  TIMESTAMPTZ,
    ended_at    TIMESTAMPTZ
);

CREATE TABLE telemetry (
    mission_id      TEXT NOT NULL REFERENCES missions,
    rover           TEXT NOT NULL,
    sequence        BIGINT NOT NULL,
    mission_seconds BIGINT NOT NULL,
    mission_clock   TEXT NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    cell_x          INT NOT NULL,
    cell_y          INT NOT NULL,
    direction       CHAR(1) NOT NULL,
    battery_percent DOUBLE PRECISION NOT NULL,
    night           BOOLEAN NOT NULL,
    distance_meters DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (mission_id, rover, sequence)
);

CREATE TABLE events (
    mission_id      TEXT NOT NULL REFERENCES missions,
    rover           TEXT NOT NULL,
    mission_seconds BIGINT NOT NULL,
    mission_clock   TEXT NOT NULL,
    type            TEXT NOT NULL,
    detail          TEXT
);
