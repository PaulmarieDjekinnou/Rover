# Rover

Simulation of Mars rovers exploring a rectangular plateau.

Each rover has a position `(x, y)` and a direction (`N`, `E`, `S`, `W`). It receives a sequence of instructions:

| Instruction | Effect |
|---|---|
| `L` | turns 90° left, without moving |
| `R` | turns 90° right, without moving |
| `M` | moves forward one cell in its current direction |

The bottom-left corner of the plateau is `(0, 0)`. North is `y + 1`.

Rovers are processed one after the other, in input order. All of them are on the plateau from the start, and two rovers can never be on the same cell:

- a move that would take the rover **off the plateau** is ignored;
- a move onto a cell **occupied by another rover** is ignored: a rover that has finished occupies its final cell, a rover that has not started yet occupies its starting cell;
- two rovers that **start on the same cell** make the input invalid.

After an ignored move, the rover stays where it is and goes on to the next instruction. Ignored moves are counted, and at the end of its mission each rover prints at most one warning per reason on the standard error output, for example:

```
Rover starting at "0 0 N": 3 move(s) ignored because they would leave the plateau.
Rover starting at "0 0 N": 1 move(s) ignored because the cell is occupied by another rover.
```

## Input file format

```
5 5
1 2 N
LMLMLMLMM
3 3 E
MMRMMRMRRM
```

- **Line 1**: coordinates of the top-right corner of the plateau.
- **Then, for each rover, two lines**: its starting position and direction, then its instructions.

Blank lines and extra spaces are ignored.

## Output

The final position and direction of each rover, one per line, in input order, on the standard output:

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
| `domain.enums` | `Direction` | the four directions, rotations and movement vector |
| | `Instruction` | the `LEFT`, `RIGHT` and `MOVE` commands (written `L`, `R` and `M` in the text format) |
| `domain` | `Position` | a cell of the plateau (`x`, `y`); independent of rovers |
| | `Plateau` | the grid boundaries |
| | `Rover` | position, direction and execution of instructions |
| | `SimulationInput`, `RoverMission` | the plateau, and each rover's mission (the rover and its instructions); checks that each rover starts on the plateau |
| | `Simulation` | moves each rover and prints its final position |
| `domain.exceptions` | `InvalidInputException` | the input content is invalid (format or rule); the message explains the error |
| `input` | `SimulationReader` | interface: a possible input for the simulation (text file, and later JSON, database…) |
| | `TextFileReader` | reads a file and passes its content to `TextFormatParser` |
| | `TextFormatParser` | reads and validates the text format, wherever the content comes from |
