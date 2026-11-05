package fr.paulm.projects.rover.telemetry;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

// Sends the telemetry to Kafka, as JSON, in three topics. The key of a rover's messages is its name, so that they all
// go to the same partition and stay in order; the messages about a mission are keyed by the mission.
// A message that cannot be sent (after the producer's retries) is counted: closing the sink reports how many.
public final class KafkaSink implements TelemetrySink {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSink.class);
    // How long to wait for Kafka to answer before the mission starts
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private final Producer<String, String> producer;
    private final AtomicLong lost = new AtomicLong();

    // A producer for this Kafka server ("localhost:9092"): every message is confirmed by the broker (acks=all),
    // a retry never doubles a message (idempotence), and messages are sent in batches.
    // Checks first that Kafka answers: otherwise, the mission would run and every message would be lost.
    public KafkaSink(String bootstrapServers) {
        this(producerFor(checked(bootstrapServers)));
    }

    // Any producer: tests give a MockProducer.
    public KafkaSink(Producer<String, String> producer) {
        this.producer = producer;
    }

    private static Producer<String, String> producerFor(String bootstrapServers) {
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.LINGER_MS_CONFIG, 5,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
    }

    private static String checked(String bootstrapServers) {
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) CONNECTION_TIMEOUT.toMillis(),
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) CONNECTION_TIMEOUT.toMillis()))) {
            admin.describeCluster().clusterId().get(CONNECTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return bootstrapServers;
        } catch (ExecutionException | TimeoutException e) {
            throw new TelemetryUnavailableException("Kafka does not answer at " + bootstrapServers, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelemetryUnavailableException("Interrupted while connecting to Kafka at " + bootstrapServers, e);
        }
    }

    @Override
    public void telemetry(RoverTelemetry message) {
        send(new ProducerRecord<>(Topics.TELEMETRY, message.rover(), TelemetryJson.write(message)));
    }

    @Override
    public void event(RoverEvent message) {
        send(new ProducerRecord<>(Topics.EVENTS, message.rover(), TelemetryJson.write(message)));
    }

    @Override
    public void status(MissionStatus message) {
        send(new ProducerRecord<>(Topics.STATUS, message.missionId(), TelemetryJson.write(message)));
    }

    // Waits for every message to be sent, then closes the producer, and reports the messages that were lost.
    @Override
    public void close() {
        producer.flush();
        producer.close();
        if (lost.get() > 0) {
            LOGGER.warn("{} telemetry message(s) could not be sent to Kafka.", lost.get());
        }
    }

    // How many messages could not be sent so far.
    public long lostMessages() {
        return lost.get();
    }

    private void send(ProducerRecord<String, String> record) {
        producer.send(record, (metadata, error) -> {
            if (error != null) {
                lost.incrementAndGet();
            }
        });
    }
}
