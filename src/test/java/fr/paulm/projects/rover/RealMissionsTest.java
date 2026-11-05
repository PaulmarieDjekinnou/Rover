package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.simulation.ClockSimulation;
import fr.paulm.projects.rover.domain.simulation.MissionResult;
import fr.paulm.projects.rover.input.Scenario;
import fr.paulm.projects.rover.input.TextFileReader;
import fr.paulm.projects.rover.mars.mission.Batteries;
import fr.paulm.projects.rover.mars.mission.Deployment;
import fr.paulm.projects.rover.mars.mission.PlanetMission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

// The sample mission against the orders of magnitude of real missions (see the references in the design notes):
// real rovers drive some tens of meters per sol on average, rarely more than a few hundred meters in a sol,
// and no more than a few hours a day. The bounds are wide on purpose: they catch a simulation that is ten times
// too fast or too slow, not a few percent.
class RealMissionsTest {

    @Test
    void roversDriveSomeTensOfMetersPerSol() throws Exception {
        PlanetMission mission = ((Scenario.OnPlanet) new TextFileReader(TestResources.path("simulations/valid/mars.txt"))
                .read()).mission();
        Deployment deployment = mission.land();
        List<MissionResult> results = new ClockSimulation(new Batteries(deployment, mission.operations()))
                .run(deployment.input());
        double sol = deployment.planet().dayLength().toMillis();
        for (MissionResult result : results) {
            double sols = result.totalTime().toMillis() / sol;
            double metersPerSol = result.distanceMeters() / sols;
            assertTrue(metersPerSol >= 10 && metersPerSol <= 150, metersPerSol + " m per sol");
            // Driving a few hours a sol at most: the drive window is 5 hours
            assertTrue(result.drivingTime().toMillis() / sol <= sols * 5 / 24.0 + 1, "too many hours of driving");
        }
    }
}
