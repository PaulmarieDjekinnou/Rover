-- The length of the sol of the planet, the dust and the panels in the telemetry, and the plan of each rover.
ALTER TABLE missions ADD COLUMN sol_seconds BIGINT;
ALTER TABLE telemetry ADD COLUMN dust_opacity DOUBLE PRECISION;
ALTER TABLE telemetry ADD COLUMN panel_cleanliness DOUBLE PRECISION;

CREATE TABLE rovers (
    mission_id         TEXT NOT NULL REFERENCES missions,
    rover              TEXT NOT NULL,
    site_latitude      DOUBLE PRECISION NOT NULL,
    site_longitude     DOUBLE PRECISION NOT NULL,
    power              TEXT NOT NULL,
    battery_watt_hours DOUBLE PRECISION NOT NULL,
    landing_seconds    BIGINT NOT NULL,
    waypoints          INT NOT NULL,
    PRIMARY KEY (mission_id, rover)
);
