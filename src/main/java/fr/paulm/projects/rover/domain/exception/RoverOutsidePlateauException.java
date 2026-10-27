package fr.paulm.projects.rover.domain.exception;

public final class RoverOutsidePlateauException extends InvalidSimulationException {
    private final int x;
    private final int y;

    public RoverOutsidePlateauException(int x, int y) {
        super("Le rover démarre hors du plateau : " + x + " " + y);
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }
}
