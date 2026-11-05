package fr.paulm.projects.rover.telemetry;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The sink against a real Kafka broker, in Docker. Disabled without Docker.
@Testcontainers(disabledWithoutDocker = true)
class KafkaSinkIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.4.0");

    @Test
    void theMessagesOfARoverArriveInOrderWithTheirKey() {
        try (KafkaSink sink = new KafkaSink(KAFKA.getBootstrapServers())) {
            sink.status(new MissionStatus("m1", MissionStatus.Type.STARTED, "f", "Mars", 1, Instant.EPOCH));
            for (int i = 0; i < 20; i++) {
                sink.telemetry(new RoverTelemetry("m1", i % 2 == 0 ? "Spirit" : "Opportunity", i / 2, i, "Sol 0 00:00",
                        0, 0, 0, 0, "N", 100, false, i));
            }
        }
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumer.subscribe(List.of(Topics.TELEMETRY, Topics.STATUS));
            long deadline = System.currentTimeMillis() + 30_000;
            while (received.size() < 21 && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
            }
        }
        assertEquals(21, received.size());
        // Within a rover (one key, one partition), the messages keep their order
        List<Long> spirit = received.stream().filter(record -> "Spirit".equals(record.key()))
                .map(record -> TelemetryJson.read(record.value(), RoverTelemetry.class).sequence()).toList();
        assertEquals(List.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), spirit);
    }
}
