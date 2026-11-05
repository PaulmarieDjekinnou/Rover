package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverPlanned;
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
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The ground station from end to end, with a real Kafka and a real PostgreSQL in Docker: a mission is sent in the
// topics as the simulator sends it, with an unreadable message, and the station archives it. Disabled without Docker.
@Testcontainers(disabledWithoutDocker = true)
class ChainIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.4.0");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private long count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    @Test
    void aMissionIsArchivedAndAnUnreadableMessageGoesToTheDeadLetters() throws Exception {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(Topics.STATUS, "m1", TelemetryJson.write(
                    new MissionStatus("m1", MissionStatus.Type.STARTED, "mission.txt", "Mars", 88_775, 2026, Instant.EPOCH))));
            producer.send(new ProducerRecord<>(Topics.PLANS, "Spirit", TelemetryJson.write(
                    new RoverPlanned("m1", "Spirit", -14.57, 175.47, "solar 1.3", 600, 0, 0))));
            producer.send(new ProducerRecord<>(Topics.EVENTS, "Spirit", TelemetryJson.write(
                    new RoverEvent("m1", "Spirit", 0, 0, "Sol 0 00:00", RoverEvent.Type.LANDED, "14.57S 175.47E"))));
            for (int i = 0; i < 5; i++) {
                producer.send(new ProducerRecord<>(Topics.TELEMETRY, "Spirit", TelemetryJson.write(new RoverTelemetry(
                        "m1", "Spirit", i, i * 100L, "Sol 0 00:00", -14.57, 175.47, 0, 0, "N", 90, false, i * 10.0, 0.5, 1))));
            }
            producer.send(new ProducerRecord<>(Topics.TELEMETRY, "Spirit", "not a message"));
        }
        Archive archive = new JdbcArchive(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Map<String, Long> archived = GroundStation.run(KAFKA.getBootstrapServers(), archive, Duration.ofSeconds(5));

        assertEquals(Map.of("m1", 5L), archived);
        assertEquals(1, count("SELECT count(*) FROM missions WHERE planet = 'Mars' AND sol_seconds = 88775"));
        assertEquals(1, count("SELECT count(*) FROM rovers WHERE power = 'solar 1.3'"));
        assertEquals(5, count("SELECT count(*) FROM telemetry"));
        assertEquals(1, count("SELECT count(*) FROM events"));

        List<ConsumerRecord<String, String>> letters = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(Topics.DEAD_LETTERS));
            long deadline = System.currentTimeMillis() + 20_000;
            while (letters.isEmpty() && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(letters::add);
            }
        }
        assertEquals(List.of("not a message"), letters.stream().map(ConsumerRecord::value).toList());
    }
}
