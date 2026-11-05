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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

// The sample mission against real missions, rover by rover. Their average distance per sol over their missions:
// - Curiosity: about 32 km by sol 4000 (late 2023), some 8 m per sol;
// - Perseverance: 45.22 km by sol 2006 (October 2026), some 22.5 m per sol;
// - Opportunity: 45.16 km by sol 5111 (June 2018), some 8.8 m per sol.
// They drive no more than a few hours a day. The bounds allow a factor of 3 either way: they catch a simulation
// that is ten times too fast or too slow, not a few percent.
class RealMissionsTest {

    private static final Map<String, Double> REAL_METERS_PER_SOL = Map.of(
            "Curiosity", 8.0, "Perseverance", 22.5, "Opportunity", 8.8);

    @Test
    void eachRoverDrivesAsFarPerSolAsItsRealModelWithinAFactorOfThree() throws Exception {
        PlanetMission mission = ((Scenario.OnPlanet) new TextFileReader(TestResources.path("simulations/valid/mars.txt"))
                .read()).mission();
        Deployment deployment = mission.land();
        List<MissionResult> results = new ClockSimulation(new Batteries(deployment, mission.operations(), mission.seed()))
                .run(deployment.input());
        double sol = deployment.planet().dayLength().toMillis();
        for (int i = 0; i < results.size(); i++) {
            String name = deployment.landed().get(i).plan().name();
            MissionResult result = results.get(i);
            double sols = result.totalTime().toMillis() / sol;
            double metersPerSol = result.distanceMeters() / sols;
            double real = REAL_METERS_PER_SOL.get(name);
            assertTrue(metersPerSol >= real / 3 && metersPerSol <= real * 3,
                    name + ": " + metersPerSol + " m per sol, against " + real + " for the real rover");
            // Driving a few hours a sol at most: the drive window is 5 hours
            assertTrue(result.drivingTime().toMillis() / sol <= sols * 5 / 24.0 + 1, name + ": too many hours of driving");
        }
    }
}
