package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.SphereGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static fr.paulm.projects.rover.mars.planet.Mars.MEAN_RADIUS_METERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinatesTest {

    // The grid of the MOLA map used by the simulation: 4 cells per degree
    private final SphereGrid mola = new SphereGrid(1440, 720);
    private final double precision = 1e-6;

    @ParameterizedTest
    @ValueSource(doubles = {-90.01, 90.01, Double.NaN})
    void rejectsALatitudeBeyondThePoles(double latitude) {
        assertThrows(InvalidInputException.class, () -> new Coordinates(latitude, 0));
    }

    @ParameterizedTest
    @CsvSource({"-90, 270", "360, 0", "725.5, 5.5"})
    void bringsTheLongitudeBackBetween0And360(double longitude, double expected) {
        assertEquals(expected, new Coordinates(0, longitude).longitude(), precision);
    }

    @Test
    void cellCentersAreHalfACellFromTheEdges() {
        assertEquals(new Coordinates(-89.875, 0.125), Coordinates.centerOf(new Position(0, 0), mola));
        assertEquals(new Coordinates(89.875, 359.875), Coordinates.centerOf(new Position(1439, 719), mola));
    }

    @ParameterizedTest
    @CsvSource({"-4.59, 137.44, 549, 341", "18.38, 77.58, 310, 433", "90, 0, 0, 719", "-90, 359.99, 1439, 0"})
    void findsTheCellOfAPoint(double latitude, double longitude, int x, int y) {
        assertEquals(new Position(x, y), new Coordinates(latitude, longitude).cellIn(mola));
    }

    @Test
    void theCenterOfACellIsInThatCell() {
        Position cell = new Position(1000, 123);
        assertEquals(cell, Coordinates.centerOf(cell, mola).cellIn(mola));
    }

    @Test
    void aQuarterOfAMeridianIsAQuarterOfTheCircumference() {
        double quarter = Math.PI * MEAN_RADIUS_METERS / 2;
        assertEquals(quarter, new Coordinates(0, 10).distanceTo(new Coordinates(90, 10), MEAN_RADIUS_METERS), 1e-3);
    }

    @Test
    void aDegreeOfLongitudeShrinksWithTheLatitude() {
        double atEquator = new Coordinates(0, 0).distanceTo(new Coordinates(0, 1), MEAN_RADIUS_METERS);
        double at60 = new Coordinates(60, 0).distanceTo(new Coordinates(60, 1), MEAN_RADIUS_METERS);
        assertEquals(0.5, at60 / atEquator, 1e-4);
    }

    @Test
    void neighbourCellsOfTheMolaGridAreAbout15KmApart() {
        Coordinates equator = Coordinates.centerOf(new Position(0, 359), mola);
        Coordinates north = Coordinates.centerOf(new Position(0, 360), mola);
        assertEquals(14_790, equator.distanceTo(north, MEAN_RADIUS_METERS), 10);
    }

    @Test
    void cellsOnBothSidesOfAPoleAreClose() {
        // Half a row to the pole, half a row back: the same distance as between two rows
        Coordinates before = Coordinates.centerOf(new Position(0, 719), mola);
        Coordinates after = Coordinates.centerOf(new Position(720, 719), mola);
        assertEquals(14_790, before.distanceTo(after, MEAN_RADIUS_METERS), 10);
    }

    @Test
    void shiftedByMovesTheGivenDistances() {
        Coordinates start = new Coordinates(-4.59, 137.44);
        Coordinates north = start.shiftedBy(10_000, 0, MEAN_RADIUS_METERS);
        Coordinates east = start.shiftedBy(0, 10_000, MEAN_RADIUS_METERS);
        assertEquals(10_000, start.distanceTo(north, MEAN_RADIUS_METERS), 1);
        assertEquals(10_000, start.distanceTo(east, MEAN_RADIUS_METERS), 1);
        assertEquals(start.longitude(), north.longitude(), precision);
        assertEquals(start.latitude(), east.latitude(), precision);
    }

    @Test
    void toStringGivesTheHemispheres() {
        assertEquals("4.59S 137.44E", new Coordinates(-4.59, 137.44).toString());
        assertEquals("18.38N 77.58E", new Coordinates(18.38, 77.58).toString());
    }
}
