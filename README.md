# Rover

[![Build](https://github.com/PaulmarieDjekinnou/Rover/actions/workflows/build.yml/badge.svg)](https://github.com/PaulmarieDjekinnou/Rover/actions/workflows/build.yml)

Simulation of Mars rovers: on a rectangular plateau (the original kata), or landed on the real planet Mars, whose relief comes from NASA data (see [Missions to Mars](#missions-to-mars)).

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
- **simultaneous** (`--simultaneous` option): turn by turn, all rovers at the same time, with no priority given by the input order. In each turn, a move is blocked when several rovers want the same cell, when two rovers would cross each other (swap their cells by moving in opposite directions), or when the cell is occupied by a rover that stays there. A rover can follow another one that leaves its cell in the same turn.

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

The simulation itself has no side effect: it returns its results, sends each turn to a listener given by `Main`, and `ConsoleReport` prints the final positions on the standard output. Warnings and errors are written with SLF4J and Logback; `simulator/src/main/resources/logback.xml` sends them to the standard error output, with the message only.

## Missions to Mars

An input file that starts with a `planet` line describes a mission to a planet: the rovers land on the real Mars (its relief measured by NASA), at a season and a local time, then drive to waypoints or follow orders, on a common clock, operated sol by sol, powered by a generator or solar panels, under the Sun, the seasons and the dust storms of Mars. The format, the model and its calibration on real missions are described in **[guide/missions.md](guide/missions.md)**.

```bash
java -jar simulator/target/rover.jar --map simulator/src/main/resources/mars-mission.txt
```

## Telemetry with Kafka

A mission can send its telemetry to Kafka (`--telemetry localhost:9092`) or to a file (`--telemetry file:mission.jsonl`); a mission with a date sends it through the link to Earth (relay orbiters, light delay, solar conjunctions). A ground station archives it in PostgreSQL, mission control follows the rovers live, in the console or on a web page, and a Kafka Streams application follows the link: each pass of an orbiter and the messages lost. Topics, messages, the archive and how to run the chain with Docker Compose: **[guide/telemetry.md](guide/telemetry.md)**.

## Requirements

- Java 25
- Maven is not needed: the Maven Wrapper (`./mvnw`) downloads the right version (3.9.16).

Every push is built and tested by GitHub Actions (`.github/workflows/build.yml`).

## Usage

```bash
./mvnw package
java -jar simulator/target/rover.jar simulator/src/main/resources/input.txt
java -jar simulator/target/rover.jar --step-by-step simulator/src/main/resources/input.txt
java -jar simulator/target/rover.jar --simultaneous simulator/src/main/resources/input.txt
java -jar simulator/target/rover.jar --step-by-step --history simulator/src/main/resources/input.txt
java -jar simulator/target/rover.jar simulator/src/main/resources/mars-mission.txt
java -jar simulator/target/rover.jar --history --map simulator/src/main/resources/mars-mission.txt
```

## Tests

```bash
./mvnw test
```

The tests read input files from `simulator/src/test/resources/simulations/`:

- `valid/<name>.txt`: input of a nominal case, run through the program and compared to the expected result in `<name>.expected.txt` (`MainTest`);
- `invalid/<name>.txt`: input of an error case, used to check that errors are reported (`TextFileReaderTest`, `MainTest`).

To add a nominal case, create both files and add the name to `MainTest.validFile`. Format errors are tested directly on strings in `TextFormatParserTest` and `MissionParserTest`. `mini-planet.txt` is a mission on the small planet drawn in `mini-planet.map.txt`; `mars.txt` is a mission on the real Mars.

`ArchitectureTest` (ArchUnit) checks the direction of the dependencies between packages: `domain` depends on nothing else; `domain.simulation` and `mars.planet` only on `domain`, and never on each other; `mars.rover` on `mars.planet`; `mars.mission` on all these; `input` on all of them; and only the root package uses everything. The build fails if a dependency goes the wrong way.

## Structure

The project has three Maven modules:

| Module | Content |
|---|---|
| `telemetry-model` | the messages the simulator sends and the ground station reads, their JSON and their topics |
| `simulator` | the simulator: rovers on a plateau, missions to a planet, and their telemetry (`rover.jar`) |
| `ground-station` | the ground station, which archives the telemetry in PostgreSQL (`ground-station.jar`), mission control, and the link monitor (Kafka Streams); it only knows the messages, never the simulator |

The integration tests (`*IT`) run against real Kafka and PostgreSQL servers (`ChainIT` sends a mission through Kafka and checks what the ground station archives), started in Docker by Testcontainers, with `./mvnw verify`; without Docker, they are skipped. GitHub Actions runs them.


All classes are in the `fr.paulm.projects.rover` package.

`domain` holds the model (how a rover moves on a surface) and does not depend on the rest. `domain.simulation` uses it to run several rovers together and report on them. `mars.planet` describes a planet and its relief, `mars.rover` how the rovers drive on it, and `mars.mission` lands them. `input` reads a text file: rovers on a plateau, or a mission to a planet.

| Package | Class | Role |
|---|---|---|
| *(root)* | `Main` | entry point: reads the arguments and the file, runs the simulation, handles errors |
| | `CommandLine`, `Mode` | the arguments: the simulation mode, the `--history`, `--map`, `--telemetry` and `--speed` options and the file; each mode with the option that selects it |
| | `ConsoleReport`, `MissionReport` | print the results on the standard output, and the warnings through the logger: the history and the final positions on a plateau; the landings, the history, the end of each mission and the maps on a planet |
| | `PacedListener` | slows a mission down to the speed given with `--speed` |
| | `ReliefMap`, `RoverTracks` | draws the relief around a cell in text, with the rovers; the cells each rover drove through |
| `domain` | `Position` | a cell of the grid (`x`, `y`); independent of rovers |
| | `Surface`, `Heading` | interface: the shape of the world, which gives the next cell and the direction a rover has when it gets there |
| | `Plateau` | a rectangular surface that wraps around like a torus |
| | `SphereGrid` | a latitude / longitude grid on a sphere: the longitude wraps around, and crossing a pole turns the rover around |
| | `Terrain`, `Obstacle` | interface: whether the ground allows a move, its length and duration; what can stop a rover (another rover or the terrain) |
| | `Direction` | the four directions, their rotations and the cell ahead of a position |
| | `Instruction` | the `LEFT`, `RIGHT`, `MOVE` and `BACK` commands (written `L`, `R`, `M` and `B` in the text format) |
| | `Rover` | position and direction; executes one instruction, and tells what would stop it (the terrain, another rover) |
| | `InvalidInputException` | the input content is invalid (format or rule); the message explains the error |
| `domain.simulation` | `SimulationInput`, `RoverMission` | the surface, its terrain, and each rover's mission (the rover and its instructions); checks that each rover starts on the surface, on its own cell |
| | `Simulation` | interface (strategy): runs the missions, sends each turn to a `TurnListener`, and returns the result of each mission |
| | `SequentialSimulation`, `StepByStepSimulation`, `SimultaneousSimulation` | the three ways of moving the rovers on a plateau: one after the other, turn by turn in input order, or turn by turn all at the same time |
| | `Pace` | interface: how long a rover takes for an instruction on the common clock, waits included, or that it can no longer act |
| | `ClockSimulation` | the rovers of a mission, all at the same time on a common clock: each instruction is decided when it starts, holds its cells while it lasts, and takes effect when it ends |
| | `Turn`, `TurnListener` | the state of all the rovers after a turn, and the observer that receives it while the simulation runs |
| | `MissionResult`, `MissionLog` | how a mission ended: the rover's final state, its moves blocked by rovers and by the terrain, the distance, the time driving and waiting, and whether it stopped; the log that counts them during the simulation |
| `mars.planet` | `Coordinates`, `Mars` | latitude and longitude, the center of a cell, the distance along the surface; the constants of Mars |
| | `Planet`, `ElevationMap` | a sphere of a given radius and day length, cut into the grid of its elevation map; the height of each cell |
| | `Orbit`, `Sun` | the year, the tilt of the axis, the sunlight and the eccentricity of a planet (Kepler's equation); the season, the local time, the height of the Sun and the sunlight on the ground |
| | `Dust`, `DustWeather` | the dust in the atmosphere: constant, or the dust season and the storms of Mars |
| | `EarthMars` | Earth and Mars on their orbits at a date: the distance between them, the light delay, and the angle between Mars and the Sun seen from Earth |
| `mars.rover` | `RoutePlanner` | the A* search of the fastest route to a waypoint |
| | `ReliefTerrain`, `Mobility` | the slopes between cells, which ones a rover can drive and how long it takes; the speed and slope limit of the rovers |
| | `PowerSource`, `PowerSystem` | a radioisotope generator or solar panels; the source and the battery of a rover, and what it uses |
| `mars.mission` | `PlanetMission`, `RoverPlan` | the planet, the settings, the seed and the plan of each rover (name, landing site, heading, orders) |
| | `Landing`, `LandingRules`, `LandingSite`, `Ellipse` | checks a landing site and draws the touchdown point in its ellipse |
| | `Operations` | the drive window and the distance per sol |
| | `Route` | what a rover carries out: its orders, or the instructions planned to its waypoints |
| | `Sky`, `LandingDate` | the season and the time at the start of the mission, and the dust in the atmosphere; the sol and local time a rover lands at |
| | `Deployment`, `Touchdown`, `LandingFailure` | the rovers on the ground: where each one landed, which ones are lost, and the simulation input of the others |
| | `Batteries` | the batteries of the rovers through the mission, and the operations: the pace that makes them wait (to recharge, for the drive window, for the next sol), or stop |
| | `RoverPositions` | follows the exact position of each rover that landed, turn by turn, across the poles too |
| | `Link` | the link to Earth of a mission with a date: the passes of the relay orbiters, how many messages a pass carries and a rover keeps, the light delay and the solar conjunctions |
| `telemetry` | `RoverTelemetry`, `RoverEvent`, `MissionStatus` | the messages a mission sends |
| | `RelayPass`, `RoverLink` | what the link monitor finds: each pass of an orbiter, and the link of each rover so far |
| | `TelemetryListener` | turns the landings, the turns and the end of a mission into messages |
| | `TelemetrySink`, `TelemetrySinks`, `KafkaSink`, `JsonLinesSink` | where the messages go: Kafka, a file, or a list in tests |
| | `Downlink` | the link to Earth between the messages and where they go: each rover keeps them until a pass of an orbiter, then they reach Earth after the light delay |
| `ground` | `GroundStation`, `Receiver`, `Archive`, `JdbcArchive` | the ground station: reads the topics and archives the messages in PostgreSQL |
| | `MissionControl`, `Dashboard`, `MissionView`, `WebPage` | mission control: follows the rovers of every mission live, in the console and on a web page |
| | `LinkMonitor`, `JsonSerde` | the link monitor, a Kafka Streams application: groups the messages by pass and by rover, and counts the ones lost |
| `input` | `SimulationReader`, `Scenario` | interface: a possible input for the simulation (text file, and later JSON, database…); what it describes, rovers on a plateau or a mission to a planet |
| | `TextFileReader` | reads a file and passes its content to the parser of its format |
| | `TextFormatParser` | reads and validates the plateau format, wherever the content comes from; errors give the line number |
| | `MissionParser`, `TextMapParser` | read and validate the mission format and the small planets drawn in text |
| | `MolaFileReader` | reads the MOLA topography of Mars |
