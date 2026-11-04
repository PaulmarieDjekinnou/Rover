package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.Terrain;
import fr.paulm.projects.rover.domain.simulation.RoverMission;
import fr.paulm.projects.rover.domain.simulation.SimulationInput;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Mars;
import fr.paulm.projects.rover.mars.planet.Planet;
import fr.paulm.projects.rover.mars.planet.Sun;
import fr.paulm.projects.rover.mars.planet.TestPlanets;
import fr.paulm.projects.rover.mars.rover.PowerSource;
import fr.paulm.projects.rover.mars.rover.PowerSystem;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatteriesTest {

    // A flat planet; it is noon on the prime meridian at the landing, at the equinox
    private final Planet planet = TestPlanets.planet(10_000, 100, "00000000", "00000000");
    private final Position noonCell = new Position(0, 0);

    private Batteries batteries(PowerSystem power, double dustOpacity) {
        Coordinates point = planet.centerOf(noonCell);
        RoverPlan plan = new RoverPlan("Rover", new LandingSite(point, Ellipse.NONE), Direction.N, List.of(), power);
        Sun sun = new Sun(planet, 0, point.longitude(), dustOpacity);
        Deployment deployment = new Deployment(planet, sun, List.of(new Touchdown(plan, point, noonCell, Optional.empty())),
                new SimulationInput(planet.grid(), Terrain.FLAT, false, List.of(new RoverMission(new Rover(noonCell, Direction.N), List.of()))));
        return new Batteries(deployment);
    }

    private Optional<Duration> drive(Batteries batteries, Duration start, Duration busy) {
        return batteries.duration(0, new Rover(noonCell, Direction.N), start, busy);
    }

    @Test
    void aShortDriveOnAFullBatteryTakesNoWait() {
        Batteries batteries = batteries(PowerSystem.DEFAULT, 0.5);
        assertEquals(Optional.of(Duration.ofHours(1)), drive(batteries, Duration.ZERO, Duration.ofHours(1)));
        // 150 W used, 110 W produced: 40 Wh less out of 2000
        assertEquals(0.98, batteries.charge(0), 1e-9);
    }

    @Test
    void aLongDriveStopsToRechargeWhenTheBatteryIsLow() {
        Batteries batteries = batteries(PowerSystem.DEFAULT, 0.5);
        // 1600 Wh to spend at 40 W: 40 hours of driving, then a wait from 20 % to 80 % at 95 W, about 12.6 hours
        Duration duration = drive(batteries, Duration.ZERO, Duration.ofHours(50)).orElseThrow();
        assertTrue(duration.compareTo(Duration.ofHours(62)) > 0 && duration.compareTo(Duration.ofHours(64)) < 0, duration.toString());
    }

    @Test
    void aSolarRoverWaitsForTheDay() {
        // A small battery: 200 Wh, 40 of them above the reserve
        Batteries batteries = batteries(new PowerSystem(new PowerSource.SolarPanels(1.3), 200), 0.5);
        // At noon, it drives a little, then has to wait for the next days to go on
        Duration duration = drive(batteries, Duration.ZERO, Duration.ofHours(8)).orElseThrow();
        assertTrue(duration.compareTo(Mars.SOL) > 0, duration.toString());
    }

    @Test
    void aRoverThatCanNeverRechargeIsStopped() {
        // Solar panels in a dust storm so thick that they never give more than the rover uses waiting
        Batteries batteries = batteries(new PowerSystem(new PowerSource.SolarPanels(1.3), 200), 20);
        assertEquals(Optional.empty(), drive(batteries, Duration.ZERO, Duration.ofHours(8)));
    }
}
