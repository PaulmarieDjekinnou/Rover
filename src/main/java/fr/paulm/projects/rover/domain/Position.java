package fr.paulm.projects.rover.domain;

// A cell of the plateau. It exists on its own: it knows nothing about rovers or about the plateau,
// which is the one that decides whether a position is valid.
public record Position(int x, int y) {

    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    // The output format of a position: "x y".
    @Override
    public String toString() {
        return x + " " + y;
    }
}
