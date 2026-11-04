package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Direction;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.mars.mission.Ellipse;
import fr.paulm.projects.rover.mars.mission.LandingSite;
import fr.paulm.projects.rover.mars.mission.RoverPlan;
import fr.paulm.projects.rover.mars.mission.Touchdown;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionReportTest {

    private static List<Touchdown> named(String... names) {
        return Arrays.stream(names)
                .map(name -> new Touchdown(new RoverPlan(name, new LandingSite(new Coordinates(0, 0), Ellipse.NONE),
                        Direction.N, List.of()), new Coordinates(0, 0), new Position(0, 0), Optional.empty()))
                .toList();
    }

    @Test
    void eachRoverHasTheFirstLetterOfItsName() {
        assertEquals(List.of('C', 'P'), MissionReport.labels(named("Curiosity", "Perseverance")));
    }

    @Test
    void aTakenLetterIsReplacedByTheNextLetterOfTheName() {
        // Sojourner takes O, its second letter; Opportunity then takes P
        assertEquals(List.of('S', 'O', 'P'), MissionReport.labels(named("Spirit", "Sojourner", "opportunity")));
    }

    @Test
    void aNameWithoutAFreeLetterTakesTheFirstFreeLetterOfTheAlphabet() {
        assertEquals(List.of('A', 'B', 'C'), MissionReport.labels(named("A", "A2", "AB")));
    }

    @Test
    void afterTheAlphabetRoversTakeDigitsThenQuestionMarks() {
        String[] names = IntStream.range(0, 38).mapToObj(i -> "R").toArray(String[]::new);
        List<Character> labels = MissionReport.labels(named(names));
        assertEquals('R', labels.get(0));
        assertEquals('Z', labels.get(25));
        assertEquals(List.of('0', '9', '?', '?'), List.of(labels.get(26), labels.get(35), labels.get(36), labels.get(37)));
    }

    @Test
    void tracksHoldTheCellsEachRoverDroveThrough() {
        RoverTracks tracks = new RoverTracks();
        tracks.onTurn(new Turn(1, List.of(new Rover(new Position(0, 1), Direction.N), new Rover(new Position(3, 3), Direction.E)), Set.of()));
        tracks.onTurn(new Turn(2, List.of(new Rover(new Position(0, 2), Direction.N), new Rover(new Position(3, 3), Direction.S)), Set.of()));
        assertEquals(List.of(new Position(0, 1), new Position(0, 2)), List.copyOf(tracks.of(0)));
        assertEquals(List.of(new Position(3, 3)), List.copyOf(tracks.of(1)));
        assertEquals(Set.of(), tracks.of(2));
    }
}
