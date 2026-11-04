package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.SphereGrid;

import java.util.Locale;

// A point on a planet, in degrees: latitude from -90 (south pole) to 90 (north pole), longitude from 0 to 360,
// positive to the east, as in the MOLA maps of Mars. A longitude outside [0, 360) is brought back into it.
public record Coordinates(double latitude, double longitude) {

    public Coordinates {
        if (!(latitude >= -90 && latitude <= 90) || !Double.isFinite(longitude)) {
            throw new InvalidInputException("Invalid coordinates: latitude " + latitude + ", longitude " + longitude
                    + ". Expected: a latitude between -90 and 90 degrees");
        }
        longitude = ((longitude % 360) + 360) % 360;
    }

    // The center of a cell of the grid. No cell is centered on a pole: the first and last rows are half a row away.
    public static Coordinates centerOf(Position cell, SphereGrid grid) {
        return new Coordinates(-90 + (cell.y() + 0.5) * 180.0 / grid.rows(),
                (cell.x() + 0.5) * 360.0 / grid.columns());
    }

    // The cell of the grid that contains this point. A point on a pole belongs to the row next to it.
    public Position cellIn(SphereGrid grid) {
        int x = Math.min((int) Math.floor(longitude * grid.columns() / 360), grid.columns() - 1);
        int y = Math.min((int) Math.floor((latitude + 90) * grid.rows() / 180), grid.rows() - 1);
        return new Position(x, y);
    }

    // The great-circle distance to another point (haversine formula), on a sphere of this radius.
    // It is the length of the shortest path on the surface, the one a rover drives.
    public double distanceTo(Coordinates other, double radiusMeters) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double sinHalfLat = Math.sin((lat2 - lat1) / 2);
        double sinHalfLon = Math.sin(Math.toRadians(other.longitude - longitude) / 2);
        double h = sinHalfLat * sinHalfLat + Math.cos(lat1) * Math.cos(lat2) * sinHalfLon * sinHalfLon;
        return 2 * radiusMeters * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    // The point reached by going the given distances north and east (negative for south and west), on a sphere
    // of this radius. Accurate for short distances (a few km), which is what a landing ellipse needs.
    public Coordinates shiftedBy(double northMeters, double eastMeters, double radiusMeters) {
        double newLatitude = latitude + Math.toDegrees(northMeters / radiusMeters);
        double newLongitude = longitude + Math.toDegrees(eastMeters / (radiusMeters * Math.cos(Math.toRadians(latitude))));
        return new Coordinates(Math.clamp(newLatitude, -90, 90), newLongitude);
    }

    // "4.59S 137.44E": two decimals, the hemisphere as a letter, and the longitude to the east.
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f%s %.2fE", Math.abs(latitude), latitude < 0 ? "S" : "N", longitude);
    }
}
