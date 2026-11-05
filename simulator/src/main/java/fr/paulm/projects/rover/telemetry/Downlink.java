package fr.paulm.projects.rover.telemetry;

import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.Link;
import fr.paulm.projects.rover.mars.mission.Touchdown;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Sun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

// The link to Earth, between the TelemetryListener and where the messages go. What a rover sends goes first to its
// memory; at each pass of a relay orbiter over its landing site, the oldest messages it holds (as many as a pass
// carries) leave, and reach Earth after the light delay, unless the Sun is between the planets. A full memory loses
// its oldest telemetry first, then its oldest events: the gap shows in the sequence numbers.
// The messages are passed on in the order they reach Earth, with the time they did, as the mission goes on (the
// telemetry follows the turns of the clock); what is still on its way at the end of the mission arrives before the
// end. The mission messages (its start, its end, the plans) are written on Earth: they go straight through, and so do
// the events of a rover lost at landing, which only light delays.
public final class Downlink implements TelemetrySink {
    private static final Logger LOGGER = LoggerFactory.getLogger(Downlink.class);
    // After a pass, the next one is looked for from a little later: a pass is a few minutes long
    private static final Duration AFTER_A_PASS = Duration.ofMinutes(1);

    private final TelemetrySink earth;
    private final Link link;
    private final Sun sun;
    private final Duration dayLength;
    private final Map<String, Memory> memories = new HashMap<>();
    private final PriorityQueue<Flight> flights =
            new PriorityQueue<>(Comparator.comparing(Flight::arrival).thenComparingLong(Flight::order));
    private Duration now = Duration.ZERO;
    private long order;
    private long lost;

    public Downlink(TelemetrySink earth, Link link, Deployment deployment) {
        this.earth = earth;
        this.link = link;
        this.sun = deployment.sun();
        this.dayLength = deployment.planet().dayLength();
        for (Touchdown touchdown : deployment.landed()) {
            memories.put(touchdown.plan().name(), new Memory(touchdown.point()));
        }
    }

    @Override
    public void telemetry(RoverTelemetry message) {
        Duration time = Duration.ofSeconds(message.missionSeconds());
        if (time.compareTo(now) > 0) {
            now = time;
        }
        memories.values().forEach(memory -> memory.passesUntil(now));
        hold(message.rover(), new Held(time, false, order++, received -> earth.telemetry(message.received(received))));
        deliverUntil(now);
    }

    @Override
    public void event(RoverEvent message) {
        Duration time = Duration.ofSeconds(message.missionSeconds());
        hold(message.rover(), new Held(time, true, order++, received -> earth.event(message.received(received))));
        deliverUntil(now);
    }

    @Override
    public void status(MissionStatus message) {
        if (message.type() == MissionStatus.Type.ENDED) {
            flush();
        }
        earth.status(message);
    }

    @Override
    public void plan(RoverPlanned message) {
        earth.plan(message);
    }

    @Override
    public void close() {
        flush();
        earth.close();
        if (lost > 0) {
            LOGGER.warn("{} telemetry message(s) were lost: the memory of a rover was full.", lost);
        }
    }

    // How many messages were lost so far, because a memory was full.
    public long lostMessages() {
        return lost;
    }

    private void hold(String rover, Held held) {
        Memory memory = memories.get(rover);
        if (memory == null) {
            fly(held, held.time());
        } else {
            memory.add(held);
        }
    }

    // Every message still on Mars leaves at the next passes, then everything on its way arrives.
    private void flush() {
        memories.values().forEach(Memory::empty);
        deliverUntil(flights.stream().map(Flight::arrival).max(Comparator.naturalOrder()).orElse(now));
    }

    private void fly(Held held, Duration sent) {
        // A message sent during a conjunction waits until the Sun is out of the way
        Duration departure = sent;
        Optional<Duration> arrival = link.arrival(departure);
        while (arrival.isEmpty()) {
            departure = departure.plusHours(1);
            arrival = link.arrival(departure);
        }
        flights.add(new Flight(arrival.get(), held.order(), held));
    }

    private void deliverUntil(Duration time) {
        while (!flights.isEmpty() && flights.peek().arrival().compareTo(time) <= 0) {
            Flight flight = flights.poll();
            flight.held().deliver().send(flight.arrival().toSeconds());
        }
    }

    private interface Delivery {
        void send(long receivedSeconds);
    }

    private record Held(Duration time, boolean event, long order, Delivery deliver) {
    }

    private record Flight(Duration arrival, long order, Held held) {
    }

    // What a rover keeps until the next pass, oldest first, and when that pass is.
    private final class Memory {
        private final Coordinates site;
        private final List<Held> messages = new ArrayList<>();
        private Duration nextPass;

        Memory(Coordinates site) {
            this.site = site;
            this.nextPass = link.nextPass(sun, site, Duration.ZERO, dayLength);
        }

        void add(Held held) {
            int at = messages.size();
            while (at > 0 && messages.get(at - 1).time().compareTo(held.time()) > 0) {
                at--;
            }
            messages.add(at, held);
            if (messages.size() > link.memoryMessages()) {
                int oldestTelemetry = 0;
                while (oldestTelemetry < messages.size() && messages.get(oldestTelemetry).event()) {
                    oldestTelemetry++;
                }
                messages.remove(oldestTelemetry < messages.size() ? oldestTelemetry : 0);
                lost++;
            }
        }

        // The passes up to that time: each one sends the oldest messages, as many as it carries, that the rover
        // already had then. The passes before the oldest message change nothing: they are skipped.
        void passesUntil(Duration time) {
            while (nextPass.compareTo(time) <= 0) {
                if (messages.isEmpty()) {
                    nextPass = link.nextPass(sun, site, time.plus(AFTER_A_PASS), dayLength);
                } else if (messages.getFirst().time().compareTo(nextPass) > 0) {
                    nextPass = link.nextPass(sun, site, messages.getFirst().time(), dayLength);
                } else {
                    pass();
                }
            }
        }

        // The passes until the memory is empty.
        void empty() {
            while (!messages.isEmpty()) {
                passesUntil(nextPass.compareTo(messages.getFirst().time()) < 0 ? messages.getFirst().time() : nextPass);
            }
        }

        private void pass() {
            Duration pass = nextPass;
            if (link.arrival(pass).isPresent()) {
                int sent = 0;
                while (sent < link.messagesPerPass() && !messages.isEmpty()
                        && messages.getFirst().time().compareTo(pass) <= 0) {
                    fly(messages.removeFirst(), pass);
                    sent++;
                }
            }
            nextPass = link.nextPass(sun, site, pass.plus(AFTER_A_PASS), dayLength);
        }
    }
}
