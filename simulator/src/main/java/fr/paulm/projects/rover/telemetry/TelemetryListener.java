package fr.paulm.projects.rover.telemetry;

import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.domain.simulation.TurnListener;
import fr.paulm.projects.rover.mars.mission.Batteries;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.MissionClock;
import fr.paulm.projects.rover.mars.mission.RoverPositions;
import fr.paulm.projects.rover.mars.mission.Touchdown;
import fr.paulm.projects.rover.mars.planet.Coordinates;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Turns a mission into telemetry, as the rovers would send it: the start of the mission and the landings, then a
// message each time a rover ends an instruction (and an event when it was blocked), then the rovers that stopped
// and the end of the mission. It follows the turns of the common clock, after RoverPositions and the batteries,
// whose state it reads.
public final class TelemetryListener implements TurnListener {
    private final String missionId;
    private final Deployment deployment;
    private final RoverPositions positions;
    private final Batteries batteries;
    private final TelemetrySink sink;
    private final Clock wallClock;
    private final List<Touchdown> landed;
    private final List<Rover> states = new ArrayList<>();
    private final long[] sequences;
    // Per rover name: the lost rovers send events too
    private final Map<String, Long> eventSequences = new HashMap<>();
    private final double[] distances;

    public TelemetryListener(String missionId, Deployment deployment, RoverPositions positions, Batteries batteries,
                             TelemetrySink sink, Clock wallClock) {
        this.missionId = missionId;
        this.deployment = deployment;
        this.positions = positions;
        this.batteries = batteries;
        this.sink = sink;
        this.wallClock = wallClock;
        this.landed = deployment.landed();
        deployment.input().missions().forEach(mission -> states.add(mission.rover()));
        this.sequences = new long[landed.size()];
        this.distances = new double[landed.size()];
    }

    // The start of the mission, then where each rover landed, or why it was lost.
    public void missionStarted(String file, long seed) {
        sink.status(new MissionStatus(missionId, MissionStatus.Type.STARTED, file, deployment.planet().name(), seed,
                wallClock.instant()));
        for (Touchdown touchdown : deployment.touchdowns()) {
            if (touchdown.landed()) {
                event(touchdown.plan().name(), touchdown.time(), RoverEvent.Type.LANDED, touchdown.point().toString());
                if (!touchdown.route().complete()) {
                    event(touchdown.plan().name(), touchdown.time(), RoverEvent.Type.NO_ROUTE,
                            touchdown.plan().waypoints().get(touchdown.route().reachedWaypoints()).toString());
                }
            } else {
                event(touchdown.plan().name(), touchdown.time(), RoverEvent.Type.LOST,
                        touchdown.failure().orElseThrow().description());
            }
        }
    }

    @Override
    public void onTurn(Turn turn) {
        if (turn.actor().isEmpty()) {
            return;
        }
        int i = turn.actor().getAsInt();
        Rover rover = turn.rovers().get(i);
        Terrain terrain = deployment.input().terrain();
        if (!rover.position().equals(states.get(i).position())) {
            distances[i] += terrain.distance(states.get(i).position(), rover.position());
        }
        states.set(i, rover);
        String name = landed.get(i).plan().name();
        if (turn.blockedRovers().contains(i)) {
            event(name, turn.time(), RoverEvent.Type.BLOCKED, "move to the " + rover.direction() + " blocked");
        }
        Coordinates point = positions.of(i);
        sink.telemetry(new RoverTelemetry(missionId, name, sequences[i]++, turn.time().toSeconds(),
                MissionClock.format(turn.time(), deployment.planet()), point.latitude(), point.longitude(),
                rover.position().x(), rover.position().y(), rover.direction().name(),
                100 * batteries.charge(i, turn.time()), deployment.sun().elevation(point, turn.time()) <= 0,
                distances[i]));
    }

    // The rovers that stopped for good, then the end of the mission.
    public void missionEnded(List<MissionResult> results, String file, long seed) {
        for (int i = 0; i < results.size(); i++) {
            int rover = i;
            results.get(i).stopTime().ifPresent(time ->
                    event(landed.get(rover).plan().name(), time, RoverEvent.Type.STOPPED, "out of energy"));
        }
        sink.status(new MissionStatus(missionId, MissionStatus.Type.ENDED, file, deployment.planet().name(), seed,
                wallClock.instant()));
    }

    private void event(String rover, Duration time, RoverEvent.Type type, String detail) {
        long sequence = eventSequences.merge(rover, 1L, Long::sum) - 1;
        sink.event(new RoverEvent(missionId, rover, sequence, time.toSeconds(),
                MissionClock.format(time, deployment.planet()), type, detail));
    }
}
