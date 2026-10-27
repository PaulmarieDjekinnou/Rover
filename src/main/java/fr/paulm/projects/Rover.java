package fr.paulm.projects;

public final class Rover {
    private int x;
    private int y;
    private Direction heading;

    public Rover(int x, int y, Direction heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    public void execute(String instructions, Plateau plateau) {
        for (char c : instructions.toCharArray()) {
            switch (c) {
                case 'L' -> heading = heading.left();
                case 'R' -> heading = heading.right();
                case 'M' -> move(plateau);
                default -> throw new IllegalArgumentException("Instruction inconnue : " + c);
            }
        }
    }

    // Un déplacement qui ferait sortir du plateau est ignoré, pour que le rover ne soit jamais perdu.
    private void move(Plateau plateau) {
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
