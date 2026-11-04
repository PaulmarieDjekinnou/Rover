package fr.paulm.projects.rover.mars.rover;

import fr.paulm.projects.rover.domain.InvalidInputException;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Sun;

import java.time.Duration;

// Where a rover gets its power from.
public sealed interface PowerSource {

    // The power the source gives, in watts, at a place and a time of the mission.
    double watts(Sun sun, Coordinates point, Duration time);

    // A radioisotope generator, like the one of Curiosity and Perseverance (about 110 W): the heat of decaying
    // plutonium, turned into electricity day and night, whatever the season or the dust.
    record Generator(double watts) implements PowerSource {

        public Generator {
            if (!(watts >= 0)) {
                throw new InvalidInputException("Invalid generator: " + watts + " W. Expected: a power that is not negative");
            }
        }

        @Override
        public double watts(Sun sun, Coordinates point, Duration time) {
            return watts;
        }
    }

    // Solar panels, like those of Spirit and Opportunity (about 1.3 m2): their power follows the Sun, so none at
    // night, little in winter or in a dust storm. They turn a fifth of the sunlight into electricity, lying flat.
    record SolarPanels(double squareMeters) implements PowerSource {

        public static final double EFFICIENCY = 0.2;

        public SolarPanels {
            if (!(squareMeters > 0)) {
                throw new InvalidInputException("Invalid solar panels: " + squareMeters + " m2. Expected: a positive area");
            }
        }

        @Override
        public double watts(Sun sun, Coordinates point, Duration time) {
            return sun.irradiance(point, time) * squareMeters * EFFICIENCY;
        }
    }
}
