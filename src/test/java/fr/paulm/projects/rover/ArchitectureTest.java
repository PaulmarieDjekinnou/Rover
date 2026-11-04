package fr.paulm.projects.rover;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

// Checks the direction of the dependencies between packages, so that the build fails if one goes the wrong way:
// the model depends on nothing else; the simulation and the planet only on the model, and never on each other;
// the rover (how it drives on a planet) on the planet; the mission (landing the rovers on a planet) on the
// simulation, the rover and the planet; the input on all of them;
// and only the application (Main and its helpers) uses everything.
@AnalyzeClasses(packages = "fr.paulm.projects.rover", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dependenciesGoFromTheApplicationToTheModel = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Application").definedBy("fr.paulm.projects.rover")
            .layer("Input").definedBy("fr.paulm.projects.rover.input")
            .layer("Mission").definedBy("fr.paulm.projects.rover.mars.mission")
            .layer("Simulation").definedBy("fr.paulm.projects.rover.domain.simulation")
            .layer("Rover").definedBy("fr.paulm.projects.rover.mars.rover")
            .layer("Planet").definedBy("fr.paulm.projects.rover.mars.planet")
            .layer("Model").definedBy("fr.paulm.projects.rover.domain")
            .whereLayer("Application").mayNotBeAccessedByAnyLayer()
            .whereLayer("Input").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Mission").mayOnlyBeAccessedByLayers("Input", "Application")
            .whereLayer("Simulation").mayOnlyBeAccessedByLayers("Mission", "Input", "Application")
            .whereLayer("Rover").mayOnlyBeAccessedByLayers("Mission", "Input", "Application")
            .whereLayer("Planet").mayOnlyBeAccessedByLayers("Rover", "Mission", "Input", "Application")
            .whereLayer("Model").mayOnlyBeAccessedByLayers("Simulation", "Planet", "Rover", "Mission", "Input", "Application");
}
