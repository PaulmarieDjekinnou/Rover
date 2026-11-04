package fr.paulm.projects.rover.telemetry;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

// Sends the telemetry to Kafka, as JSON, in three topics. The key of a rover's messages is its name, so that they all
// go to the same partition and stay in order; the messages about a mission are keyed by the mission.
public final class KafkaSink implements TelemetrySink {

    public static final String TELEMETRY_TOPIC = "rover.telemetry";
    public static final String EVENTS_TOPIC = "rover.events";
    public static final String STATUS_TOPIC = "mission.status";

    private final Producer<String, String> producer;

    // A producer for this Kafka server ("localhost:9092"): every message is confirmed by the broker (acks=all),
    // a retry never doubles a message (idempotence), and messages are sent in batches.
    public KafkaSink(String bootstrapServers) {
        this(new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.LINGER_MS_CONFIG, 5,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
    }

    // Any producer: tests give a MockProducer.
    public KafkaSink(Producer<String, String> producer) {
        this.producer = producer;
    }

    @Override
    public void telemetry(RoverTelemetry message) {
        producer.send(new ProducerRecord<>(TELEMETRY_TOPIC, message.rover(), TelemetryJson.write(message)));
    }

    @Override
    public void event(RoverEvent message) {
        producer.send(new ProducerRecord<>(EVENTS_TOPIC, message.rover(), TelemetryJson.write(message)));
    }

    @Override
    public void status(MissionStatus message) {
        producer.send(new ProducerRecord<>(STATUS_TOPIC, message.missionId(), TelemetryJson.write(message)));
    }

    // Waits for every message to be sent, then closes the producer.
    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
