package fr.paulm.projects.rover.telemetry;

// The telemetry cannot be sent: Kafka does not answer, or the file cannot be written.
// Main reports it before the mission starts.
public final class TelemetryUnavailableException extends RuntimeException {

    public TelemetryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
