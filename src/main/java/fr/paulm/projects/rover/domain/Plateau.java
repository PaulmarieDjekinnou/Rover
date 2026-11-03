package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.exceptions.InvalidInputException;

public final class Plateau {
    private final int maxX;
    private final int maxY;

    public Plateau(int maxX, int maxY) {
        if (maxX < 0 || maxY < 0) {
            throw new InvalidInputException("Invalid plateau size: \"" + maxX + " " + maxY + "\". Dimensions cannot be negative");
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean contains(Position position) {
        return position.x() >= 0 && position.x() <= maxX
                && position.y() >= 0 && position.y() <= maxY;
    }
}
