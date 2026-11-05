-- When each message reached Earth, in seconds of the mission: later than mission_seconds when the mission simulates
-- the link to Earth. The messages archived before knew no delay: they reached Earth when they were sent.
ALTER TABLE telemetry ADD COLUMN received_seconds BIGINT;
UPDATE telemetry SET received_seconds = mission_seconds;
ALTER TABLE telemetry ALTER COLUMN received_seconds SET NOT NULL;
ALTER TABLE events ADD COLUMN received_seconds BIGINT;
UPDATE events SET received_seconds = mission_seconds;
ALTER TABLE events ALTER COLUMN received_seconds SET NOT NULL;
