package fr.paulm.projects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private List<String> printedLines() {
        return out.toString().lines().toList();
    }

    @Test
    void printsFinalPositionOfEachRoverInOrder() {
        SimulationInput input = new SimulationInput(new Plateau(5, 5), List.of(
                new RoverInstructions(new Rover(1, 2, Direction.N), "LMLMLMLMM"),
                new RoverInstructions(new Rover(3, 3, Direction.E), "MMRMMRMRRM")));
        new Simulation(input).run();
        assertEquals(List.of("1 3 N", "5 1 E"), printedLines());
    }

    @Test
    void roversShareThePlateauLimits() {
        SimulationInput input = new SimulationInput(new Plateau(2, 2), List.of(
                new RoverInstructions(new Rover(0, 0, Direction.N), "MMMM"),
                new RoverInstructions(new Rover(0, 0, Direction.E), "MMMM")));
        new Simulation(input).run();
        assertEquals(List.of("0 2 N", "2 0 E"), printedLines());
    }

    @Test
    void noRoversPrintsNothing() {
        new Simulation(new SimulationInput(new Plateau(5, 5), List.of())).run();
        assertEquals("", out.toString());
    }

    // Bout en bout : chaque fichier simulations/valid/<nom>.txt est lu, simulé et comparé à <nom>.expected.txt

    @ParameterizedTest
    @ValueSource(strings = {"sample", "blank-lines", "same-cell", "stuck-at-edge", "no-rovers"})
    void validFile(String name) throws Exception {
        List<String> lines = TestResources.read("simulations/valid/" + name + ".txt");
        List<String> expected = TestResources.read("simulations/valid/" + name + ".expected.txt");
        new Simulation(new InputParser().parse(lines)).run();
        assertEquals(expected, printedLines());
    }
}
