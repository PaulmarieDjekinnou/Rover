package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.RoverLink;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;
import fr.paulm.projects.rover.telemetry.Topics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The link monitor against a real Kafka in Docker, exactly once, which needs the transactions of the broker: the
// messages of a rover go in, with an unreadable one, and its link comes out. Disabled without Docker.
@Testcontainers(disabledWithoutDocker = true)
class LinkMonitorIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.4.0");

    @Test
    void theLinkOfARoverComesOutOfTheMessagesItSent(@TempDir Path state) throws Exception {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            // Message 2 was lost on the way
            for (long sequence : List.of(0L, 1L, 3L)) {
                producer.send(new ProducerRecord<>(Topics.TELEMETRY, "Spirit", TelemetryJson.write(new RoverTelemetry("m1",
                        "Spirit", sequence, sequence * 100, "Sol 0", 0, 0, 1, 1, "N", 90, false, 0, 0.5, 1).received(1_000))));
            }
            producer.send(new ProducerRecord<>(Topics.EVENTS, "Spirit", "not a message"));
        }
        Properties properties = LinkMonitor.properties(KAFKA.getBootstrapServers());
        properties.put(StreamsConfig.STATE_DIR_CONFIG, state.toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaStreams streams = new KafkaStreams(LinkMonitor.topology(), properties);
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                     ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                     ConsumerConfig.GROUP_ID_CONFIG, "test",
                     ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                     // Only what the link monitor has committed
                     ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed",
                     ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                     ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            streams.start();
            consumer.subscribe(List.of(Topics.LINKS));
            RoverLink link = null;
            long deadline = System.currentTimeMillis() + 60_000;
            while ((link == null || link.telemetry() < 3) && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.key().equals("m1/Spirit")) {
                        link = TelemetryJson.read(record.value(), RoverLink.class);
                    }
                }
            }
            assertEquals(new RoverLink("m1", "Spirit", 3, 0, 1, 0, 3, -1, 1_000, 1_000), link);
        }
    }
}
