package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.RelayPass;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverLink;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;
import fr.paulm.projects.rover.telemetry.Topics;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The topology of the link monitor, without Kafka: the test driver runs it on messages given one by one.
class LinkMonitorTest {

    private final TopologyTestDriver driver = new TopologyTestDriver(LinkMonitor.topology(), LinkMonitor.properties("unused:9092"));
    private final TestInputTopic<String, String> telemetry =
            driver.createInputTopic(Topics.TELEMETRY, Serdes.String().serializer(), Serdes.String().serializer());
    private final TestInputTopic<String, RoverEvent> events =
            driver.createInputTopic(Topics.EVENTS, Serdes.String().serializer(), new JsonSerde<>(RoverEvent.class).serializer());

    @AfterEach
    void close() {
        driver.close();
    }

    private void telemetry(long sequence, long sent, long received) {
        telemetry.pipeInput("Spirit", TelemetryJson.write(new RoverTelemetry("m1", "Spirit",
                sequence, sent, "Sol 0", 0, 0, 1, 1, "N", 90, false, 0, 0.5, 1).received(received)));
    }

    private void event(long sequence, long sent, long received) {
        events.pipeInput("Spirit", new RoverEvent("m1", "Spirit", sequence, sent, "Sol 0", RoverEvent.Type.BLOCKED_BY_TERRAIN,
                "blocked").received(received));
    }

    // The last record of each key, as a compacted topic keeps it.
    private <T> Map<String, T> latest(String topic, Class<T> type) {
        return driver.createOutputTopic(topic, Serdes.String().deserializer(), new JsonSerde<>(type).deserializer())
                .readKeyValuesToMap();
    }

    @Test
    void theMessagesThatReachEarthTogetherMakeAPass() {
        telemetry(0, 100, 5_000);
        telemetry(1, 200, 5_000);
        event(0, 150, 5_000);
        telemetry(2, 6_000, 9_000);
        Map<String, RelayPass> passes = latest(Topics.PASSES, RelayPass.class);
        assertEquals(new RelayPass("m1", "Spirit", 5_000, 2, 1, 100, 200), passes.get("m1/Spirit/5000"));
        assertEquals(new RelayPass("m1", "Spirit", 9_000, 1, 0, 6_000, 6_000), passes.get("m1/Spirit/9000"));
        assertEquals(2, passes.size());
    }

    @Test
    void theGapsInTheSequenceNumbersAreTheMessagesLostWhateverTheOrder() {
        telemetry(0, 100, 1_000);
        telemetry(3, 400, 2_000);
        event(1, 300, 2_000);
        RoverLink link = latest(Topics.LINKS, RoverLink.class).get("m1/Spirit");
        assertEquals(new RoverLink("m1", "Spirit", 2, 1, 2, 1, 3, 1, 2_000, 1_700), link);
        // A message that was only late fills its gap
        telemetry(1, 200, 3_000);
        link = latest(Topics.LINKS, RoverLink.class).get("m1/Spirit");
        assertEquals(1, link.lostTelemetry());
        assertEquals(2_800, link.longestWaitSeconds());
    }

    @Test
    void anUnreadableMessageIsSkipped() {
        telemetry.pipeInput("Spirit", "not a message");
        telemetry(0, 100, 1_000);
        assertEquals(1, latest(Topics.LINKS, RoverLink.class).get("m1/Spirit").telemetry());
    }
}
