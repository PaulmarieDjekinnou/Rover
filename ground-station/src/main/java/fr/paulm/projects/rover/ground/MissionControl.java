package fr.paulm.projects.rover.ground;

import fr.paulm.projects.rover.telemetry.MissionStatus;
import fr.paulm.projects.rover.telemetry.RoverEvent;
import fr.paulm.projects.rover.telemetry.RoverTelemetry;
import fr.paulm.projects.rover.telemetry.TelemetryJson;
import fr.paulm.projects.rover.telemetry.Topics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Mission control: follows the rovers of every mission live, in the console, from the telemetry in Kafka, and on a
// web page with --web port.
// Each mission control has its own consumer group and starts from the latest messages: it shows what happens now,
// while the ground station archives everything in its own group. The screen is redrawn every second.
//
//   java -cp ground-station.jar fr.paulm.projects.rover.ground.MissionControl localhost:9092 [--web 8080]
public final class MissionControl {

    private MissionControl() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1 && !(args.length == 3 && args[1].equals("--web"))) {
            System.err.println("Usage: MissionControl kafka-server [--web port]");
            System.exit(1);
        }
        PrintStream out = System.out;
        Dashboard dashboard = new Dashboard();
        // The web page reads the dashboard from the server's threads, the consumer writes it: they take turns
        WebPage page = args.length == 3 ? new WebPage(Integer.parseInt(args[2]), () -> {
            synchronized (dashboard) {
                return dashboard.json();
            }
        }) : null;
        if (page != null) {
            page.start();
            System.err.println("Mission control on http://localhost:" + args[2]);
        }
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, args[0],
                ConsumerConfig.GROUP_ID_CONFIG, "mission-control-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            Thread main = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                consumer.wakeup();
                try {
                    main.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            consumer.subscribe(List.of(Topics.STATUS, Topics.TELEMETRY, Topics.EVENTS));
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    synchronized (dashboard) {
                        for (ConsumerRecord<String, String> record : records) {
                            try {
                                switch (record.topic()) {
                                    case Topics.TELEMETRY -> dashboard.telemetry(TelemetryJson.read(record.value(), RoverTelemetry.class));
                                    case Topics.STATUS -> dashboard.status(TelemetryJson.read(record.value(), MissionStatus.class));
                                    default -> dashboard.event(TelemetryJson.read(record.value(), RoverEvent.class));
                                }
                            } catch (IllegalArgumentException e) {
                                // An unreadable message: the ground station sends it to the dead letters
                            }
                        }
                        // Clear the screen (ANSI codes), then draw the rovers
                        out.print("\033[H\033[2J");
                        dashboard.render().forEach(out::println);
                        out.flush();
                    }
                }
            } catch (WakeupException e) {
                // Ctrl+C
            }
            if (page != null) {
                page.stop();
            }
        }
    }
}
