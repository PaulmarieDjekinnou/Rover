package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.Topics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// The ground station: reads the telemetry of the missions from Kafka and archives it in PostgreSQL.
// It reads the three topics in the "ground-station" consumer group, and tells Kafka what it has read (commits its
// offsets) only once the messages are in the database: if it stops in between, it reads them again (at least once).
// A message it cannot read does not stop it: the message goes to the dead letters topic, with the reason in its
// headers, and the station reads on.
// It runs until Ctrl+C, or until no message has come for a while (--until-idle seconds).
//
//   java -jar ground-station.jar localhost:9092 jdbc:postgresql://localhost:5432/rover
//
// The database user and password come from ROVER_DB_USER and ROVER_DB_PASSWORD (rover and rover by default).
public final class GroundStation {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroundStation.class);
    private static final String GROUP = "ground-station";

    private GroundStation() {
    }

    public static void main(String[] args) {
        if (args.length != 2 && !(args.length == 4 && args[2].equals("--until-idle"))) {
            LOGGER.error("Usage: GroundStation kafka-server jdbc-url [--until-idle seconds]");
            System.exit(1);
        }
        Duration idle = args.length == 4 ? Duration.ofSeconds(Long.parseLong(args[3])) : null;
        String user = System.getenv().getOrDefault("ROVER_DB_USER", "rover");
        String password = System.getenv().getOrDefault("ROVER_DB_PASSWORD", "rover");
        run(args[0], new JdbcArchive(args[1], user, password), idle);
    }

    // Reads Kafka into the archive until Ctrl+C, or until no message has come for idle (when given).
    // Returns the number of telemetry messages archived per mission.
    public static Map<String, Long> run(String kafka, Archive archive, Duration idle) {
        try (archive;
             KafkaProducer<String, String> deadLetters = new KafkaProducer<>(Map.of(
                     ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka,
                     ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                     ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));
             KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                     ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka,
                     ConsumerConfig.GROUP_ID_CONFIG, GROUP,
                     ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                     ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                     ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                     ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            Thread main = Thread.currentThread();
            Thread hook = new Thread(() -> {
                consumer.wakeup();
                try {
                    main.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Runtime.getRuntime().addShutdownHook(hook);
            Receiver receiver = new Receiver(archive);
            consumer.subscribe(List.of(Topics.STATUS, Topics.PLANS, Topics.EVENTS, Topics.TELEMETRY));
            LOGGER.info("Ground station listening to {}", kafka);
            Instant lastMessage = Instant.now();
            try {
                while (idle == null || Duration.between(lastMessage, Instant.now()).compareTo(idle) < 0) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            receiver.receive(record.topic(), record.value());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Unreadable message in {}, sent to {}: {}", record.topic(), Topics.DEAD_LETTERS,
                                    e.getMessage());
                            ProducerRecord<String, String> letter =
                                    new ProducerRecord<>(Topics.DEAD_LETTERS, record.key(), record.value());
                            letter.headers().add("topic", record.topic().getBytes(StandardCharsets.UTF_8));
                            letter.headers().add("reason", String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
                            deadLetters.send(letter);
                        }
                    }
                    if (!records.isEmpty()) {
                        archive.commit();
                        deadLetters.flush();
                        consumer.commitSync();
                        lastMessage = Instant.now();
                    }
                }
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (WakeupException e) {
                // Ctrl+C: stop reading, and close the consumer and the database properly
            }
            receiver.telemetryPerMission().forEach((mission, count) ->
                    LOGGER.info("Mission {}: {} telemetry message(s) archived", mission, count));
            return receiver.telemetryPerMission();
        }
    }
}
