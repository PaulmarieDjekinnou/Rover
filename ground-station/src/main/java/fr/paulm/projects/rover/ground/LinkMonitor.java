package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.RelayPass;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverLink;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.Topics;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.BuiltInDslStoreSuppliers;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

// The link monitor: a Kafka Streams application that follows the link to Earth, from the telemetry and the events.
// The messages of a rover that left at the same pass of an orbiter all reach Earth at the same time: grouped by that
// time, they give each pass (topic rover.passes, key mission/rover/received seconds). Grouped by rover, they give its
// link so far (topic rover.links, key mission/rover), with the messages lost on the way: the gaps in the sequence
// numbers, whatever order the messages arrive in.
// Each message updates its pass and its rover, and the new state is written at once: the last record of a key is the
// complete one, and both topics are compacted. A message is counted once, even if the application stops in the
// middle (exactly once: what it reads, its state and what it writes are committed together). Its state is kept in
// memory, and rebuilt from Kafka (the changelog topics) when it starts again. A message it cannot read is logged and
// skipped: the ground station already sends it to the dead letters.
//
//   java -cp ground-station.jar fr.paulm.projects.rover.ground.LinkMonitor localhost:9092
public final class LinkMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkMonitor.class);
    private static final String APPLICATION_ID = "link-monitor";

    private LinkMonitor() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: LinkMonitor kafka-server");
            System.exit(1);
        }
        KafkaStreams streams = new KafkaStreams(topology(), properties(args[0]));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> streams.close(Duration.ofSeconds(10))));
        streams.start();
        LOGGER.info("Following the link to Earth from {}: topics {} and {}", args[0], Topics.PASSES, Topics.LINKS);
    }

    static Properties properties(String kafka) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafka);
        properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        properties.put(StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG, BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers.class);
        properties.put(StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
        // Every store and internal topic is named (see store)
        properties.put(StreamsConfig.ENSURE_EXPLICIT_INTERNAL_RESOURCE_NAMING_CONFIG, true);
        // The behaviour of Kafka 5.0, without the warning at each start (there is no global table here anyway)
        properties.put(StreamsConfig.PROCESSING_EXCEPTION_HANDLER_GLOBAL_ENABLED_CONFIG, true);
        return properties;
    }

    static Topology topology() {
        StreamsBuilder builder = new StreamsBuilder();
        Serde<String> keys = Serdes.String();
        Serde<RelayPass> passes = new JsonSerde<>(RelayPass.class);
        Serde<RoverLink> links = new JsonSerde<>(RoverLink.class);
        KStream<String, RoverTelemetry> telemetry =
                builder.stream(Topics.TELEMETRY, Consumed.with(keys, new JsonSerde<>(RoverTelemetry.class)));
        KStream<String, RoverEvent> events = builder.stream(Topics.EVENTS, Consumed.with(keys, new JsonSerde<>(RoverEvent.class)));

        // Each message is a pass of its own, then the messages that reached Earth at the same time are added up
        telemetry.mapValues(message -> pass(message)).merge(events.mapValues(message -> pass(message)))
                .groupBy((rover, pass) -> pass.missionId() + "/" + pass.rover() + "/" + pass.receivedSeconds(),
                        Grouped.with("passes", keys, passes))
                .reduce(LinkMonitor::add, LinkMonitor.<RelayPass>store("passes", passes))
                .toStream()
                .to(Topics.PASSES, Produced.with(keys, passes));

        // The same for the link of each rover
        telemetry.mapValues(message -> link(message)).merge(events.mapValues(message -> link(message)))
                .groupBy((rover, link) -> link.missionId() + "/" + link.rover(), Grouped.with("links", keys, links))
                .reduce(LinkMonitor::add, LinkMonitor.<RoverLink>store("links", links))
                .toStream()
                .to(Topics.LINKS, Produced.with(keys, links));
        return builder.build();
    }

    // A state store with a name of its own, like its changelog topic: the names Kafka Streams would give change when
    // the topology changes, and the state kept in Kafka would then be lost.
    private static <V> Materialized<String, V, KeyValueStore<Bytes, byte[]>> store(String name, Serde<V> values) {
        return Materialized.<String, V, KeyValueStore<Bytes, byte[]>>as(name).withKeySerde(Serdes.String()).withValueSerde(values);
    }

    static RelayPass pass(RoverTelemetry m) {
        return new RelayPass(m.missionId(), m.rover(), m.receivedSeconds(), 1, 0, m.missionSeconds(), m.missionSeconds());
    }

    static RelayPass pass(RoverEvent m) {
        return new RelayPass(m.missionId(), m.rover(), m.receivedSeconds(), 0, 1, m.missionSeconds(), m.missionSeconds());
    }

    static RelayPass add(RelayPass pass, RelayPass message) {
        return new RelayPass(pass.missionId(), pass.rover(), pass.receivedSeconds(), pass.telemetry() + message.telemetry(),
                pass.events() + message.events(), Math.min(pass.oldestSeconds(), message.oldestSeconds()),
                Math.max(pass.newestSeconds(), message.newestSeconds()));
    }

    // A sequence of -1: no message of that kind yet.
    static RoverLink link(RoverTelemetry m) {
        return link(m.missionId(), m.rover(), 1, 0, m.sequence(), -1, m.receivedSeconds(), m.receivedSeconds() - m.missionSeconds());
    }

    static RoverLink link(RoverEvent m) {
        return link(m.missionId(), m.rover(), 0, 1, -1, m.sequence(), m.receivedSeconds(), m.receivedSeconds() - m.missionSeconds());
    }

    static RoverLink add(RoverLink link, RoverLink message) {
        return link(link.missionId(), link.rover(), link.telemetry() + message.telemetry(), link.events() + message.events(),
                Math.max(link.lastTelemetrySequence(), message.lastTelemetrySequence()),
                Math.max(link.lastEventSequence(), message.lastEventSequence()),
                Math.max(link.lastReceivedSeconds(), message.lastReceivedSeconds()),
                Math.max(link.longestWaitSeconds(), message.longestWaitSeconds()));
    }

    // The sequence numbers start at 0: up to the last one, every number not received is a message lost.
    private static RoverLink link(String missionId, String rover, long telemetry, long events, long lastTelemetrySequence,
                                  long lastEventSequence, long lastReceivedSeconds, long longestWaitSeconds) {
        return new RoverLink(missionId, rover, telemetry, events, lastTelemetrySequence + 1 - telemetry,
                lastEventSequence + 1 - events, lastTelemetrySequence, lastEventSequence, lastReceivedSeconds,
                longestWaitSeconds);
    }
}
