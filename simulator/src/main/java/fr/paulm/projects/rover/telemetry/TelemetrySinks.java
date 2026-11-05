package fr.paulm.projects.rover.telemetry;

import java.nio.file.Path;

// Where the --telemetry option sends the messages: "file:mission.jsonl" writes them to a file (JSON Lines),
// anything else is the address of a Kafka server ("localhost:9092").
public final class TelemetrySinks {

    static final String FILE_PREFIX = "file:";

    private TelemetrySinks() {
    }

    public static TelemetrySink open(String target) {
        if (target.startsWith(FILE_PREFIX)) {
            return new JsonLinesSink(Path.of(target.substring(FILE_PREFIX.length())));
        }
        return new KafkaSink(target);
    }
}
