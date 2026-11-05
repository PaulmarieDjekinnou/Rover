package fr.paulm.projects.rover.telemetry;

import java.util.ArrayList;
import java.util.List;

// Keeps the messages sent, for tests.
public final class InMemorySink implements TelemetrySink {
    public final List<RoverTelemetry> telemetry = new ArrayList<>();
    public final List<RoverEvent> events = new ArrayList<>();
    public final List<MissionStatus> statuses = new ArrayList<>();
    public boolean closed;

    @Override
    public void telemetry(RoverTelemetry message) {
        telemetry.add(message);
    }

    @Override
    public void event(RoverEvent message) {
        events.add(message);
    }

    @Override
    public void status(MissionStatus message) {
        statuses.add(message);
    }

    @Override
    public void close() {
        closed = true;
    }
}
