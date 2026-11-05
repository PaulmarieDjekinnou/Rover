package fr.paulm.projects.rover.mars.planet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

// The dust of Mars over the years: clear skies in the northern spring and summer, more dust in the dust season
// (Ls 180 to 360, the southern spring and summer, when Mars is closest to the Sun), and dust storms.
// - the season: the clear opacity doubles at the height of the dust season (Ls 270);
// - regional storms: two a year, in the dust season, for a few weeks each (8 to 16 degrees of Ls);
// - global storms: one year in three, a storm that covers the planet for months (35 to 70 degrees of Ls), as in
//   2001, 2007 and 2018 (the one that ended Opportunity's mission).
// The storms are drawn from the seed of the mission, year by year: the same seed always gives the same weather.
// These rates are orders of magnitude, not a fit to observations.
public final class DustWeather implements Dust {

    private static final double REGIONAL_OPACITY = 1.5;
    private static final double GLOBAL_OPACITY = 4.5;

    // A storm in a given year of the mission: from a season, for a span of seasons, with its extra opacity
    private record Storm(long year, double startLs, double spanLs, double opacity) {

        // Its extra opacity in that season: it builds up in the first fifth of its span, then fades out.
        double opacityAt(long currentYear, double solarLongitude) {
            double into = ((solarLongitude - startLs) % 360 + 360) % 360;
            if (currentYear != year || into > spanLs) {
                return 0;
            }
            double fraction = into / spanLs;
            return opacity * (fraction < 0.2 ? fraction / 0.2 : (1 - fraction) / 0.8);
        }
    }

    private final double clearOpacity;
    private final Planet planet;
    private final long seed;
    private final List<Storm> storms = new ArrayList<>();
    private long yearsDrawn;

    public DustWeather(double clearOpacity, Planet planet, long seed) {
        this.clearOpacity = Dust.constant(clearOpacity).opticalDepth(Duration.ZERO, 0);
        this.planet = planet;
        this.seed = seed;
    }

    @Override
    public double opticalDepth(Duration time, double solarLongitudeDegrees) {
        long year = (long) Math.floor(time.toMillis() / (planet.orbit().yearInSols() * planet.dayLength().toMillis()));
        drawUpTo(year);
        double season = Math.max(0, Math.sin(Math.toRadians(solarLongitudeDegrees - 180)));
        double opacity = clearOpacity * (1 + season);
        for (Storm storm : storms) {
            opacity += storm.opacityAt(year, solarLongitudeDegrees);
        }
        return opacity;
    }

    // Whether a storm is going on (an opacity above that of the season alone).
    public boolean stormAt(Duration time, double solarLongitudeDegrees) {
        double season = Math.max(0, Math.sin(Math.toRadians(solarLongitudeDegrees - 180)));
        return opticalDepth(time, solarLongitudeDegrees) > clearOpacity * (1 + season) + 0.5;
    }

    private void drawUpTo(long year) {
        while (yearsDrawn <= year) {
            SplittableRandom random = new SplittableRandom(seed * 1_000_003 + yearsDrawn);
            for (int i = 0; i < 2; i++) {
                storms.add(new Storm(yearsDrawn, 180 + random.nextDouble(160), 8 + random.nextDouble(8), REGIONAL_OPACITY));
            }
            if (random.nextInt(3) == 0) {
                storms.add(new Storm(yearsDrawn, 190 + random.nextDouble(110), 35 + random.nextDouble(35), GLOBAL_OPACITY));
            }
            yearsDrawn++;
        }
    }
}
