package fr.paulm.projects.rover.telemetry;

// Where the telemetry of a mission goes: Kafka, or a list in tests. Closing it makes sure every message is sent.
public interface TelemetrySink extends AutoCloseable {

    void telemetry(RoverTelemetry message);

    void event(RoverEvent message);

    void status(MissionStatus message);

    void plan(RoverPlanned message);

    @Override
    void close();
}
