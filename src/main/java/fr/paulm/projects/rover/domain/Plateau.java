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

    public boolean contains(int x, int y) {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }
}
