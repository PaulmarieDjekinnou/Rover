# Rover

Simulation of Mars rovers exploring a rectangular plateau.

Each rover has a position `(x, y)` and a direction (`N`, `E`, `S`, `W`). It receives a sequence of instructions:

| Instruction | Effect |
|---|---|
| `L` | turns 90° left, without moving |
| `R` | turns 90° right, without moving |
| `M` | moves forward one cell in its current direction |
| `B` | moves back one cell, keeping its direction |

The bottom-left corner of the plateau is `(0, 0)`. North is `y + 1`.

The plateau is a small planet: it **wraps around**, like a torus. A rover that moves beyond one edge comes back on the opposite edge, on the same row or column, with the same direction. For example, on a `5 5` plateau, a rover in `4 5 N` that moves ends in `4 0 N`. A rover can never leave the plateau.

All rovers are on the plateau from the start, and two rovers can never be on the same cell:

- a move (forward or back) that would **collide with another rover** is blocked. This also holds across an edge;
- two rovers that **start on the same cell** make the input invalid.

The rovers can move in three ways:

- **sequential** (default): one after the other, in input order. Each rover executes all its instructions before the next one starts, so a rover that has finished occupies its final cell, and a rover that has not started yet occupies its starting cell;
- **step by step** (`--step-by-step` option): turn by turn. In each turn, every rover executes its next instruction, in input order. A rover that has no instruction left stays on its cell;
- **simultaneous** (`--simultaneous` option): turn by turn, all rovers at the same time, with no priority given by the input order. In each turn, a move is blocked when several rovers want the same cell, when two rovers would swap their cells, or when the cell is occupied by a rover that stays there. A rover can follow another one that leaves its cell in the same turn.

The three modes give the same result as long as the rovers never get in each other's way.

After a blocked move, the rover stays where it is and goes on to the next instruction. Blocked moves are counted, and one warning per blocked rover is printed on the standard error output, with the rover's number in input order, for example:

```
Rover 2: 3 move(s) blocked by another rover.
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

Blank lines and extra spaces are ignored. Every rover needs an instruction line: a rover without one is reported on its position line.

## Output

The final position and direction of each rover, one per line, in input order, on the standard output:

```
1 3 N
5 1 E
```

With the `--history` option, the state of every rover after each turn is printed as soon as the turn is over, one line per turn, with the rovers whose move was blocked in that turn:

```
Turn 1: 0 1 N | 0 3 S
Turn 2: 0 2 N | 0 3 S
Turn 3: 0 2 N | 0 3 S (blocked: 1)
```

What a turn is depends on the mode: one instruction of one rover in sequential mode, one instruction of every rover in the other two modes.

If something goes wrong, the program prints an error message on the standard error output and exits with code 1. Format errors give the line number in the file (blank lines included), for example:

```
Invalid input: Line 4: Invalid direction: "X". Expected: N, E, S or W
Cannot read file "input.txt": file not found
```

An unexpected error (a bug) is reported as `Unexpected error: …`, followed by its stack trace.

The simulation itself has no side effect: it returns its results, sends each turn to a listener given by `Main`, and `ConsoleReport` prints the final positions on the standard output. Warnings and errors are written with SLF4J and Logback; `src/main/resources/logback.xml` sends them to the standard error output, with the message only.

## Requirements

- Java 25
- Maven

## Usage

```bash
mvn package
java -jar target/rover.jar src/main/resources/input.txt
java -jar target/rover.jar --step-by-step src/main/resources/input.txt
java -jar target/rover.jar --simultaneous src/main/resources/input.txt
java -jar target/rover.jar --step-by-step --history src/main/resources/input.txt
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

`domain` holds the model (how a rover moves on the plateau) and does not depend on the rest. `domain.simulation` uses it to run several rovers together and report on them. `input` builds a `SimulationInput` from a text file.

| Package | Class | Role |
|---|---|---|
| *(root)* | `Main` | entry point: reads the arguments and the file, runs the simulation, handles errors |
| | `CommandLine`, `Mode` | the arguments: the simulation mode, the `--history` option and the file; each mode with the option that selects it |
| | `ConsoleReport` | prints the history and the final positions on the standard output, and the warnings through the logger |
| `domain` | `Position` | a cell of the plateau (`x`, `y`); independent of rovers |
| | `Plateau` | the grid boundaries; wraps positions around its edges |
| | `Direction` | the four directions, their rotations and the cells ahead of and behind a position |
| | `Instruction` | the `LEFT`, `RIGHT`, `MOVE` and `BACK` commands (written `L`, `R`, `M` and `B` in the text format) |
| | `Rover` | position and direction; executes one instruction, unless it would collide with another rover |
| | `InvalidInputException` | the input content is invalid (format or rule); the message explains the error |
| `domain.simulation` | `SimulationInput`, `RoverMission` | the plateau, and each rover's mission (the rover and its instructions); checks that each rover starts on the plateau, on its own cell |
| | `Simulation` | interface (strategy): runs the missions, sends each turn to a `TurnListener`, and returns the result of each mission |
| | `SequentialSimulation`, `StepByStepSimulation`, `SimultaneousSimulation` | the three ways of moving the rovers: one after the other, turn by turn in input order, or turn by turn all at the same time |
| | `Turn`, `TurnListener` | the state of all the rovers after a turn, and the observer that receives it while the simulation runs |
| | `MissionResult` | how a mission ended: the rover's final state and the number of blocked moves |
| `input` | `SimulationReader` | interface: a possible input for the simulation (text file, and later JSON, database…) |
| | `TextFileReader` | reads a file and passes its content to `TextFormatParser` |
| | `TextFormatParser` | reads and validates the text format, wherever the content comes from; errors give the line number |
