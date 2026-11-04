package fr.paulm.projects.rover.domain;

// Receives the state of the rovers after each turn, while the simulation runs (observer).
// The domain does not know what the listener does with it: display it, store it, send it…
@FunctionalInterface
public interface TurnListener {

    // A listener that ignores every turn, for when nobody needs the history.
    TurnListener NONE = turn -> {
    };

    void onTurn(Turn turn);
}
