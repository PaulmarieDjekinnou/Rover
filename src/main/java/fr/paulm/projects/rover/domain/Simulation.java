package fr.paulm.projects.rover.domain;

public final class Simulation {
    private final SimulationInput input;

    public Simulation(SimulationInput input) {
        this.input = input;
    }

    // Déplace chaque rover selon ses instructions, dans l'ordre du fichier,
    // et affiche sa position finale dès qu'il a terminé.
    public void run() {
        for (RoverInstructions r : input.rovers()) {
            r.rover().execute(r.instructions(), input.plateau());
            System.out.println(r.rover());
        }
    }
}
