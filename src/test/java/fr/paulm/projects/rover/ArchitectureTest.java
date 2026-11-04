package fr.paulm.projects.rover;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

// Checks the direction of the dependencies between packages, so that the build fails if one goes the wrong way:
// the model depends on nothing else, the simulation only on the model, the input on the domain,
// and only the application (Main and its helpers) uses everything.
@AnalyzeClasses(packages = "fr.paulm.projects.rover", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dependenciesGoFromTheApplicationToTheModel = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Application").definedBy("fr.paulm.projects.rover")
            .layer("Input").definedBy("fr.paulm.projects.rover.input")
            .layer("Simulation").definedBy("fr.paulm.projects.rover.domain.simulation")
            .layer("Model").definedBy("fr.paulm.projects.rover.domain")
            .whereLayer("Application").mayNotBeAccessedByAnyLayer()
            .whereLayer("Input").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Simulation").mayOnlyBeAccessedByLayers("Input", "Application")
            .whereLayer("Model").mayOnlyBeAccessedByLayers("Simulation", "Input", "Application");
}
