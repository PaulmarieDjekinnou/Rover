package fr.paulm.projects.rover;

import fr.paulm.projects.rover.domain.Position;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.domain.simulation.TurnListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Follows the simulation and writes down the cells each rover drives through, in input order,
// to draw the paths on the maps.
final class RoverTracks implements TurnListener {
    private final List<Set<Position>> tracks = new ArrayList<>();

    @Override
    public void onTurn(Turn turn) {
        for (int i = 0; i < turn.rovers().size(); i++) {
            if (tracks.size() <= i) {
                tracks.add(new LinkedHashSet<>());
            }
            tracks.get(i).add(turn.rovers().get(i).position());
        }
    }

    // The cells a rover went through, after its landing cell, in the order it reached them.
    Set<Position> of(int rover) {
        return rover < tracks.size() ? tracks.get(rover) : Set.of();
    }
}
