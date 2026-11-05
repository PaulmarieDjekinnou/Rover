package fr.paulm.projects.rover.telemetry;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaSinkTest {

    private final MockProducer<String, String> producer =
            new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
    private final KafkaSink sink = new KafkaSink(producer);

    @Test
    void eachKindOfMessageGoesToItsTopicKeyedByRoverOrMission() {
        sink.status(new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", 1, Instant.EPOCH));
        sink.event(new RoverEvent("m1", "Curiosity", 0, 0, "Sol 0 00:00", RoverEvent.Type.LANDED, "4.74S 137.45E"));
        RoverTelemetry telemetry = new RoverTelemetry("m1", "Curiosity", 0, 60, "Sol 0 00:01", 0, 0, 0, 0, "N", 99, false, 0);
        sink.telemetry(telemetry);
        List<ProducerRecord<String, String>> sent = producer.history();
        assertEquals(List.of("mission.status", "rover.events", "rover.telemetry"), sent.stream().map(ProducerRecord::topic).toList());
        assertEquals(List.of("m1", "Curiosity", "Curiosity"), sent.stream().map(ProducerRecord::key).toList());
        assertEquals(telemetry, TelemetryJson.read(sent.get(2).value(), RoverTelemetry.class));
    }

    @Test
    void closingSendsEverythingAndClosesTheProducer() {
        sink.close();
        assertTrue(producer.closed());
    }

    @Test
    void messagesThatCannotBeSentAreCounted() {
        MockProducer<String, String> failing = new MockProducer<>(false, null, new StringSerializer(), new StringSerializer());
        KafkaSink sink = new KafkaSink(failing);
        sink.event(new RoverEvent("m1", "Spirit", 0, 0, "Sol 0 00:00", RoverEvent.Type.LANDED, "here"));
        sink.event(new RoverEvent("m1", "Spirit", 1, 0, "Sol 0 00:00", RoverEvent.Type.STOPPED, "out of energy"));
        failing.errorNext(new RuntimeException("broker down"));
        failing.completeNext();
        assertEquals(1, sink.lostMessages());
    }
}
