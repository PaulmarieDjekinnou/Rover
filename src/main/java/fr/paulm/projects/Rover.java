package fr.paulm.projects;

public final class Rover {
    private final Plateau plateau;
    private int x;
    private int y;
    private Direction heading;

    public Rover(Plateau plateau, int x, int y, Direction heading) {
        if (!plateau.contains(x, y)) {
            throw new IllegalArgumentException("Le rover démarre hors du plateau : " + x + " " + y);
        }
        this.plateau = plateau;
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public void execute(String instructions) {
        for (char c : instructions.toCharArray()) {
            switch (c) {
                case 'L' -> heading = heading.left();
                case 'R' -> heading = heading.right();
                case 'M' -> move();
                default -> throw new IllegalArgumentException("Instruction inconnue : " + c);
            }
        }
    }

    // Un déplacement qui ferait sortir du plateau est ignoré, pour que le rover ne soit jamais perdu.
    private void move() {
        int nx = x + heading.dx;
        int ny = y + heading.dy;
        if (plateau.contains(nx, ny)) {
            x = nx;
            y = ny;
        }
    }

    @Override
    public String toString() {
        return x + " " + y + " " + heading;
    }
}
