package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// A map of the relief around a cell, drawn in text, north up. Each cell is a character from the lowest (.)
// to the highest (@) elevation of the window; marks (the rovers) are drawn over the relief.
// The window wraps around in longitude, like the planet, but is never wider than it. Rows beyond a pole are left out.
final class ReliefMap {

    // From the lowest to the highest elevation of the window
    private static final String RELIEF = ".:-=+*#%@";

    private final int width;
    private final int height;

    // The size of the window, in cells. Odd sizes put the center cell in the middle.
    ReliefMap(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // The legend, then one line per row of the window, from north to south.
    List<String> draw(Planet planet, Position center, Map<Position, Character> marks) {
        int columns = planet.grid().columns();
        int rows = planet.grid().rows();
        List<List<Position>> window = new ArrayList<>();
        for (int y = center.y() + height / 2; y >= center.y() - height / 2; y--) {
            if (y < 0 || y >= rows) {
                continue;
            }
            // Never wider than the planet, so that no cell is drawn twice
            int cells = Math.min(width, columns);
            List<Position> row = new ArrayList<>();
            for (int x = center.x() - cells / 2; x < center.x() - cells / 2 + cells; x++) {
                row.add(new Position(Math.floorMod(x, columns), y));
            }
            window.add(row);
        }
        int lowest = window.stream().flatMap(List::stream).mapToInt(planet::elevation).min().orElseThrow();
        int highest = window.stream().flatMap(List::stream).mapToInt(planet::elevation).max().orElseThrow();

        List<String> lines = new ArrayList<>();
        lines.add("Relief from " + lowest + " m (" + RELIEF.charAt(0) + ") to " + highest + " m ("
                + RELIEF.charAt(RELIEF.length() - 1) + "), north up, " + window.getFirst().size() + " x " + window.size() + " cells:");
        for (List<Position> row : window) {
            StringBuilder line = new StringBuilder();
            for (Position cell : row) {
                line.append(marks.getOrDefault(cell, symbol(planet.elevation(cell), lowest, highest)));
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private char symbol(int elevation, int lowest, int highest) {
        if (highest == lowest) {
            return RELIEF.charAt(0);
        }
        int level = (int) ((long) (elevation - lowest) * RELIEF.length() / (highest - lowest + 1));
        return RELIEF.charAt(level);
    }
}
