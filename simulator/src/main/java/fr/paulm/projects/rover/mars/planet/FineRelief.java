package fr.paulm.projects.rover.mars.planet;

import fr.paulm.projects.rover.domain.Position;

import java.util.HashMap;
import java.util.Map;

// A finer relief derived from a coarse one, without data to download: each fine cell is the bilinear interpolation
// of the coarse relief at its center, plus a detail drawn from the seed (a fractal value noise of a few octaves,
// about 100 m on the largest scale). The large scale relief is that of the data; the detail is generated, not
// measured. Cells are computed when they are first asked for, then kept: a mission only uses the cells around its
// rovers, never the hundreds of millions of cells of the whole planet.
public final class FineRelief implements Relief {

    private static final double[] OCTAVE_CELLS = {16, 8, 4, 2};
    private static final double[] OCTAVE_METERS = {60, 30, 15, 8};

    private final Relief coarse;
    private final int factor;
    private final long seed;
    private final Map<Position, Integer> computed = new HashMap<>();

    // factor: how many fine cells per coarse cell, along each axis.
    public FineRelief(Relief coarse, int factor, long seed) {
        this.coarse = coarse;
        this.factor = factor;
        this.seed = seed;
    }

    @Override
    public int columns() {
        return coarse.columns() * factor;
    }

    @Override
    public int rows() {
        return coarse.rows() * factor;
    }

    @Override
    public int elevation(Position cell) {
        return computed.computeIfAbsent(cell, this::compute);
    }

    private int compute(Position cell) {
        // The center of the fine cell, in coarse cells (the centers of coarse cells are at .5)
        double x = (cell.x() + 0.5) / factor - 0.5;
        double y = (cell.y() + 0.5) / factor - 0.5;
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        double below = lerp(coarseAt(x0, y0), coarseAt(x0 + 1, y0), fx);
        double above = lerp(coarseAt(x0, y0 + 1), coarseAt(x0 + 1, y0 + 1), fx);
        double detail = 0;
        for (int octave = 0; octave < OCTAVE_CELLS.length; octave++) {
            detail += OCTAVE_METERS[octave] * noise(cell.x() / OCTAVE_CELLS[octave], cell.y() / OCTAVE_CELLS[octave], octave);
        }
        return (int) Math.round(lerp(below, above, fy) + detail);
    }

    // The coarse elevation, the longitude wrapping around and the latitude kept within the poles.
    private double coarseAt(int x, int y) {
        return coarse.elevation(new Position(Math.floorMod(x, coarse.columns()), Math.clamp(y, 0, coarse.rows() - 1)));
    }

    // Value noise from -1 to 1: random values on a lattice, smoothly interpolated.
    private double noise(double x, double y, int octave) {
        long x0 = (long) Math.floor(x);
        long y0 = (long) Math.floor(y);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        return lerp(lerp(lattice(x0, y0, octave), lattice(x0 + 1, y0, octave), fx),
                lerp(lattice(x0, y0 + 1, octave), lattice(x0 + 1, y0 + 1, octave), fx), fy);
    }

    // A random value from -1 to 1 for a lattice point, always the same for the same seed (a hash, SplitMix64).
    private double lattice(long x, long y, int octave) {
        long z = seed * 0x9E3779B97F4A7C15L + x * 0xBF58476D1CE4E5B9L + y * 0x94D049BB133111EBL + octave;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
