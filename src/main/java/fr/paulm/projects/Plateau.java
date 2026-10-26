package fr.paulm.projects;

public final class Plateau {
    private final int maxX;
    private final int maxY;

    public Plateau(int maxX, int maxY) {
        if (maxX < 0 || maxY < 0) {
            throw new IllegalArgumentException("Taille de plateau invalide : " + maxX + " " + maxY);
        }
        this.maxX = maxX;
        this.maxY = maxY;
    }

    boolean contains(int x, int y) {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }
}
