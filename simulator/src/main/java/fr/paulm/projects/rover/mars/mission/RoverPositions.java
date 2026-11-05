package fr.paulm.projects.rover.mars.mission;

import fr.paulm.projects.rover.domain.Rover;
import fr.paulm.projects.rover.domain.simulation.Turn;
import fr.paulm.projects.rover.domain.simulation.TurnListener;
import fr.paulm.projects.rover.mars.planet.Coordinates;
import fr.paulm.projects.rover.mars.planet.Planet;

import java.util.ArrayList;
import java.util.List;

// Where each rover that landed exactly is, followed turn by turn. The simulation moves rovers from cell to cell;
// the exact position starts where the rover touched down, and makes the same step as its cell each time it moves:
// - to a neighbouring cell, it moves by the difference between the two centers, so it keeps its place in its cell;
// - across a pole (the surface turns the rover around), it goes on to the other side, half a turn of longitude
//   away: the pole is between the two cells, so a rover north of the center of its cell ends south of the center
//   of the next one, and the other way round.
public final class RoverPositions implements TurnListener {
    private final Planet planet;
    private final List<Rover> states = new ArrayList<>();
    private final List<Coordinates> points = new ArrayList<>();

    public RoverPositions(Deployment deployment) {
        this.planet = deployment.planet();
        List<Touchdown> landed = deployment.landed();
        for (int i = 0; i < landed.size(); i++) {
            states.add(deployment.input().missions().get(i).rover());
            points.add(landed.get(i).point());
        }
    }

    @Override
    public void onTurn(Turn turn) {
        for (int i = 0; i < turn.rovers().size(); i++) {
            Rover before = states.get(i);
            Rover after = turn.rovers().get(i);
            if (!after.position().equals(before.position())) {
                points.set(i, step(points.get(i), before, after));
            }
            states.set(i, after);
        }
    }

    // Where a rover that landed is now, in input order among the rovers that landed.
    public Coordinates of(int rover) {
        return points.get(rover);
    }

    // A move never changes the direction of a rover, unless it crosses a pole.
    private Coordinates step(Coordinates point, Rover from, Rover to) {
        Coordinates fromCenter = planet.centerOf(from.position());
        Coordinates toCenter = planet.centerOf(to.position());
        if (to.direction() != from.direction()) {
            return new Coordinates(2 * fromCenter.latitude() - point.latitude(), point.longitude() + 180);
        }
        return new Coordinates(point.latitude() + toCenter.latitude() - fromCenter.latitude(),
                point.longitude() + toCenter.longitude() - fromCenter.longitude());
    }
}
