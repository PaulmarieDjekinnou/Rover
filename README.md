# Rover

Simulation of Mars rovers exploring a rectangular plateau.

Each rover has a position `(x, y)` and a heading (`N`, `E`, `S`, `W`). It receives a sequence of instructions:

| Instruction | Effect |
|---|---|
| `L` | turns 90° left, without moving |
| `R` | turns 90° right, without moving |
| `M` | moves forward one cell in its current heading |

The bottom-left corner of the plateau is `(0, 0)`. North is `y + 1`.

A move that would take the rover off the plateau is ignored: the rover stays where it is and goes on to the next instruction. A warning is printed on the standard error output, for example `Rover at "0 5 N" cannot move to "0 6": move ignored.` Rovers are processed one after the other and independently, with no collision detection.

## Input file format

```
5 5
1 2 N
LMLMLMLMM
3 3 E
MMRMMRMRRM
```

- **Line 1**: coordinates of the top-right corner of the plateau.
- **Then, for each rover, two lines**: its starting position and heading, then its instructions.

Blank lines and extra spaces are ignored.

## Output

The final position and heading of each rover, one per line, in input order, on the standard output:

```
1 3 N
5 1 E
```

If something goes wrong, the program prints an error message on the standard error output and exits with code 1, for example:

```
Invalid input: Invalid direction: "X". Expected: N, E, S or W
```

Results and errors are written with SLF4J and Logback. `src/main/resources/logback.xml` sends the final positions to the standard output and everything else to the standard error output, with the message only.

## Requirements

- Java 21
- Maven

## Usage

```bash
mvn package
java -jar target/rover.jar src/main/resources/input.txt
```

## Tests

```bash
mvn test
```

The tests read input files from `src/test/resources/simulations/`:

- `valid/<name>.txt`: input of a nominal case, run through the program and compared to the expected result in `<name>.expected.txt` (`MainTest`);
- `invalid/<name>.txt`: input of an error case, used to check that errors are reported (`TextFileReaderTest`, `MainTest`).

To add a nominal case, create both files and add the name to `MainTest.validFile`. Format errors are tested directly on strings in `TextFormatParserTest`.

## Structure

All classes are in the `fr.paulm.projects.rover` package.

| Package | Class | Role |
|---|---|---|
| *(root)* | `Main` | entry point: reads the file given as argument and handles errors |
| `domain.enums` | `Direction` | the four headings, rotations and movement vector |
| | `Instruction` | the `LEFT`, `RIGHT` and `MOVE` commands (written `L`, `R` and `M` in the text format) |
| `domain` | `Plateau` | the grid boundaries |
| | `Rover` | position, heading and execution of instructions |
| | `SimulationInput`, `RoverMission` | the plateau, and each rover's mission (the rover and its instructions); checks that each rover starts on the plateau |
| | `Simulation` | moves each rover and prints its final position |
| `domain.exceptions` | `InvalidInputException` | the input content is invalid (format or rule); the message explains the error |
| `input` | `SimulationReader` | interface: a possible input for the simulation (text file, and later JSON, database…) |
| | `TextFileReader` | reads a file and passes its content to `TextFormatParser` |
| | `TextFormatParser` | reads and validates the text format, wherever the content comes from |
