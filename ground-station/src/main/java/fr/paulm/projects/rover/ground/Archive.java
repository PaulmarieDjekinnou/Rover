package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;

// Where the ground station keeps what it receives: PostgreSQL, or a list in tests.
// The messages of one poll are written together: commit makes them durable, before the station tells Kafka
// it has read them.
public interface Archive extends AutoCloseable {

    void status(MissionStatus message);

    void telemetry(RoverTelemetry message);

    void event(RoverEvent message);

    void commit();

    @Override
    void close();
}
