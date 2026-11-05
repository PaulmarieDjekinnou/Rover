-- Events are numbered per rover, like the telemetry: an event read twice is archived once.
ALTER TABLE events ADD COLUMN sequence BIGINT NOT NULL DEFAULT 0;
ALTER TABLE events ALTER COLUMN sequence DROP DEFAULT;
ALTER TABLE events ADD PRIMARY KEY (mission_id, rover, sequence);
