package fr.paulm.projects.rover.domain;

import fr.paulm.projects.rover.domain.exception.InvalidSimulationException;

public final class Plateau {
    private final int maxX;
    private final int maxY;

    public Plateau(int maxX, int maxY) {
        if (maxX < 0 || maxY < 0) {
            throw new InvalidSimulationException("Taille de plateau invalide : " + maxX + " " + maxY);
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }
}
