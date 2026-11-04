package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.ElevationMap;
import fr.paulm.projects.rover.mars.planet.Mars;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

// Reads the topography of Mars measured by the MOLA laser altimeter of Mars Global Surveyor (NASA, public domain):
// the MEGDR files of the PDS Geosciences Node. Their format is described in the .lbl file next to the data:
// 16-bit signed integers in big-endian order (MSB_INTEGER), in meters above the areoid, one line per latitude
// from the north pole, each line from longitude 0 eastwards.
public final class MolaFileReader {

    // The whole planet at 4 cells per degree (about 15 km per cell at the equator), shipped with the program
    private static final String MARS_RESOURCE = "/mars/megt90n000cb.img";
    private static final int MARS_COLUMNS = 1440;
    private static final int MARS_ROWS = 720;

    // Mars, from the MOLA map shipped with the program.
    public Planet mars() {
        try (InputStream in = MolaFileReader.class.getResourceAsStream(MARS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + MARS_RESOURCE);
            }
            return new Planet("Mars", Mars.MEAN_RADIUS_METERS, Mars.SOL, Mars.ORBIT, read(in, MARS_COLUMNS, MARS_ROWS));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // A MOLA map of the given size. The file must hold exactly one value per cell.
    // Its first line is the northernmost one, so the lines are reversed: in the simulation, row 0 is in the south.
    public ElevationMap read(InputStream in, int columns, int rows) throws IOException {
        byte[] bytes = in.readAllBytes();
        long expected = 2L * columns * rows;
        if (bytes.length != expected) {
            throw new InvalidInputException("Invalid MOLA map: " + bytes.length + " bytes instead of " + expected
                    + " for " + columns + " columns and " + rows + " rows");
        }
        ShortBuffer values = ByteBuffer.wrap(bytes).asShortBuffer();
        short[] elevations = new short[columns * rows];
        for (int line = 0; line < rows; line++) {
            values.get(line * columns, elevations, (rows - 1 - line) * columns, columns);
        }
        return new ElevationMap(columns, rows, elevations);
    }
}
