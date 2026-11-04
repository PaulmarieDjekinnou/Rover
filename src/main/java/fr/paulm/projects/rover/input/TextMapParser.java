package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.ElevationMap;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.List;

// A small planet drawn in text, for tests and experiments:
//
//   radius-km 10
//   meters-per-digit 1000
//   00000000
//   00090000
//
// Then one line per row of the grid, from north to south as on a map, with one digit per cell: the elevation
// in steps of meters-per-digit. All rows have the same, even, number of cells.
public final class TextMapParser {

    public Planet parse(String name, String content) {
        List<Line> lines = Line.nonBlankLines(content);
        if (lines.size() < 3) {
            throw new InvalidInputException("Empty map: expected radius-km, meters-per-digit, then the rows of the map");
        }
        double radiusMeters = number(lines.get(0), "radius-km") * 1_000;
        if (!(radiusMeters > 0)) {
            throw lines.get(0).error("Invalid radius: expected a positive number of km");
        }
        int metersPerDigit = (int) number(lines.get(1), "meters-per-digit");
        List<Line> rows = lines.subList(2, lines.size());
        // Each row is checked here first, to report the right line
        int columns = rows.getFirst().text().length();
        for (Line row : rows) {
            if (row.text().length() != columns || !row.text().chars().allMatch(Character::isDigit)) {
                throw row.error("Invalid map row: \"" + row.text() + "\". Expected: " + columns + " digits");
            }
        }
        try {
            ElevationMap map = ElevationMap.fromDigits(rows.stream().map(Line::text).toList(), metersPerDigit);
            return new Planet(name, radiusMeters, map);
        } catch (InvalidInputException e) {
            throw rows.getFirst().error(e.getMessage());
        }
    }

    private double number(Line line, String key) {
        if (!line.keyword().equals(key)) {
            throw line.error("Expected: " + key + " followed by a number");
        }
        return line.decimal();
    }
}
