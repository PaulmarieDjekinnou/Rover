package fr.paulm.projects;

public enum Direction {
    N(0, 1), E(1, 0), S(0, -1), W(-1, 0);

    final int dx;
    final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    Direction left() {
        return values()[(ordinal() + 3) % 4];
    }

    Direction right() {
        return values()[(ordinal() + 1) % 4];
    }
}
