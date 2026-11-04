package fr.paulm.projects.rover.mars.mission;

// Why a rover is lost at landing.
public enum LandingFailure {
    ON_ANOTHER_ROVER("landed on another rover (less than " + (int) Landing.CLEARANCE_METERS + " m away)"),
    NO_WAY_OUT("landed where it cannot drive anywhere");

    private final String description;

    LandingFailure(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
