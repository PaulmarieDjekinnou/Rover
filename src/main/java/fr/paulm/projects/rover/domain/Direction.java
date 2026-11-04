package fr.paulm.projects.rover.domain;

import java.util.Arrays;
import java.util.Optional;

// Each direction knows its movement vector and its two neighbouring directions.
public enum Direction {
    N(0, 1) {
        @Override
        public Direction left() {
            return W;
        }

        @Override
        public Direction right() {
            return E;
        }
    },
    E(1, 0) {
        @Override
        public Direction left() {
            return N;
        }

        @Override
        public Direction right() {
            return S;
        }
    },
    S(0, -1) {
        @Override
        public Direction left() {
            return E;
        }

        @Override
        public Direction right() {
            return W;
        }
    },
    W(-1, 0) {
        @Override
        public Direction left() {
            return S;
        }

        @Override
        public Direction right() {
            return N;
        }
    };

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    // The direction written with this code (N, E, S or W), if any. The caller decides how to report an unknown code.
    public static Optional<Direction> fromCode(String code) {
        return Arrays.stream(values())
                .filter(direction -> direction.name().equals(code))
                .findFirst();
    }

    // The cell ahead of the given position, one step in this direction.
    public Position ahead(Position position) {
        return position.shiftedBy(dx, dy);
    }

    // The cell behind the given position, one step in the opposite direction.
    public Position behind(Position position) {
        return position.shiftedBy(-dx, -dy);
    }

    // The direction after a half turn.
    public Direction opposite() {
        return right().right();
    }

    // The direction after a 90° turn to the left.
    public abstract Direction left();

    // The direction after a 90° turn to the right.
    public abstract Direction right();
}
