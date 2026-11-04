# Rover

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

The simulation itself has no side effect: it returns its results, sends each turn to a listener given by `Main`, and `ConsoleReport` prints the final positions on the standard output. Warnings and errors are written with SLF4J and Logback; `src/main/resources/logback.xml` sends them to the standard error output, with the message only.

## Missions to Mars

An input file that starts with a `planet` line describes a mission to a planet. The rovers are first sent down to the surface, then carry out their orders all at the same time, on a common clock, on a sphere with real relief.

```
planet mars
start-ls 150
landing-time 15:00
seed 2026

rover Curiosity
site -4.59 137.44
ellipse 20 7 0
heading S
orders MMMLMM
```

| Line | Meaning |
|---|---|
| `planet mars` | the planet: `mars`, or `map <file>` for a small planet drawn in text, in a file next to the mission (see below) |
| `seed 42` | optional: the seed of the random draws (0 by default). The same seed always gives the same landings |
| `max-slope 10` | optional: the steepest slope the rovers can drive on, in degrees (10 by default) |
| `speed 0.04` | optional: the speed of the rovers on flat ground, in m/s (0.04 by default, about Curiosity's top speed) |
| `turn-minutes 5` | optional: how long a turn on the spot takes, in minutes (5 by default) |
| `landing-max-elevation -1000` | optional: the highest landing site allowed, in meters (-1000 by default) |
| `landing-max-latitude 30` | optional: the farthest landing site from the equator, in degrees (30 by default) |
| `start-ls 150` | optional: the season at the landing, as a solar longitude Ls in degrees: 0 at the northern spring equinox, 90 at the northern summer solstice (0 by default) |
| `landing-time 15:00` | optional: the local solar time at the start of the mission, at the site of the first rover (12:00 by default) |
| `dust-opacity 0.5` | optional: the optical depth of the dust in the atmosphere, which weakens sunlight: about 0.5 on a clear day, above 4 in a global dust storm (0.5 by default) |
| `rover <name>` | starts the block of a rover |
| `site <latitude> <longitude>` | where the rover aims: latitude negative in the south, longitude from 0 to 360 to the east |
| `ellipse <major> <minor> <azimuth>` | optional: the landing ellipse, axes in km, azimuth of the major axis in degrees from north (none by default) |
| `heading N` | the direction the rover faces once on the ground |
| `orders MMRM` | optional: the instructions, as on the plateau |
| `power rtg 110` | optional: the power source, a radioisotope generator of 110 W (like Curiosity's), or `solar 1.3` for 1.3 m2 of solar panels (like Opportunity's) (`rtg 110` by default) |
| `battery 2000` | optional: the capacity of the battery, in Wh (2000 by default) |
| `landing 3 14:00` | optional: the rover lands on sol 3 of the mission, when it is 14:00 local time at its site (by default, at the start of the mission, with the first rover) |

Lines starting with `#` are comments. The whole file is checked before anything lands, and errors give the line number.

**The planet.** `planet mars` uses the topography measured by the MOLA laser altimeter of Mars Global Surveyor (NASA, public domain), at 4 cells per degree: 1440 × 720 cells of about 15 km at the equator, in `src/main/resources/mars/` with the description of its format (`.lbl`). The planet is a sphere cut into a latitude / longitude grid: `x` is the column (the longitude, to the east) and `y` the row (the latitude, from the south pole). Going east or west wraps around the planet; going north from the last row crosses the north pole: the rover arrives on the same row, half a turn of longitude away, now facing south (and likewise at the south pole).

**The landing.** A site is refused if it is too high (the parachute needs enough atmosphere), too far from the equator, or next to a slope the rover could not drive. Each rover then touches down at a random point of its ellipse. A rover that touches down less than 100 m from another rover, or in a cell it cannot leave, is lost: it takes no part in the simulation.

**The drive.** A move is blocked by the terrain when the slope between the two cells is steeper than `max-slope`, uphill or downhill. Distances are measured in meters between the centers of the cells, along the surface; climbing slows the rover down, to half its speed at the steepest slope. Unlike on the plateau, several rovers can be in the same cell: a cell is about 15 km wide, so they are kilometers apart, and rovers never block each other. Each instruction takes its time: a move lasts its distance divided by the speed, a turn on the spot `turn-minutes` (5 minutes by default), and a move blocked by the terrain no time. A rover starts its next instruction as soon as the previous one is over, and all the rovers share the same clock: the simulation handles the ends of instructions in time order. The three modes of the plateau do not apply to a mission: their options are ignored, with a warning. On a plateau, where instructions take no time, this clock would give exactly the step-by-step mode.

**The Sun.** The planet turns on itself once a sol and goes around the Sun once a year (668.6 sols on Mars), on an axis tilted by 25.19 degrees, which makes the seasons. From the season (Ls), the time and the place, the simulation knows the local solar time and how high the Sun is. Sunlight on the ground is about 586 W/m2 above the atmosphere of Mars, weakened by the dust, all the more as the Sun is low.

**The energy.** Each rover has a battery, full at the landing, charged by its power source: a generator gives the same power day and night, solar panels turn a fifth of the sunlight they get into power, so nothing at night, little in winter or in a dust storm. A rover uses 150 W when it drives or turns, and 15 W when it waits. It works while its battery holds more than a fifth of its capacity; below, it waits and recharges until the battery is four fifths full, then goes on, so a solar rover drives by day and waits at night. Waiting takes time on the common clock. A rover whose battery does not recharge in a whole year of waiting stops for good, like Spirit in 2010: its other orders are left undone.

Each rover enters the simulation when it lands, on the common clock: rovers sent at different dates land on different sols. The output gives the landing of each rover, on the mission clock (the sol, from 0, and the time of day), then, for each rover that landed, its final position, its cell and direction, the distance driven and the time spent driving, the time spent waiting for energy, in sols (Martian days of 24 h 39 min 35 s), and the charge of its battery. Both give the local time and the Sun:

```
Curiosity landed on Sol 0 00:00 at 4.74S 137.45E (cell 549 341), 9.1 km from its target, local time 15:00, Sun 42 degrees high
Perseverance landed on Sol 2 04:52 at 18.34N 77.61E (cell 310 433), 3.0 km from its target, local time 15:53, Sun 34 degrees high
Opportunity landed on Sol 5 07:46 at 1.95S 354.47E (cell 1417 352), 0.0 km from its target, local time 13:15, Sun 67 degrees high
Curiosity: 5.49S 137.95E (cell 551 338) facing E, 73.8 km driven in 25.3 sols, 10.4 sols waiting for energy, battery 69%, local time 06:32, Sun 8 degrees high
Perseverance: 18.84N 76.86E (cell 307 435) facing N, 71.7 km driven in 21.0 sols, 8.3 sols waiting for energy, battery 34%, local time 22:29, night
Opportunity: 1.95S 354.97E (cell 1419 352) facing E, 29.6 km driven in 8.3 sols, 87.8 sols waiting for energy, battery 45%, local time 15:21, Sun 39 degrees high
```

A rover that ran out of energy is reported with the time it stopped: `Spirit stopped, out of energy, on Sol 668 17:39: ...`. The instruction it was on is not done: it stays in its cell. Warnings name the rovers: `Climber: 1 move(s) blocked by the terrain.` With `--history`, a line is printed each time an instruction ends, in time order: the time of the common clock, as the sol since the start of the mission (from 0) and the time of day (a sol cut into 24 hours, like the local time on Mars), then each rover that has landed by then, with its position and direction, and the names of the blocked rovers:

```
Sol 7 09:51: Curiosity 4.99S 137.45E S | Perseverance 18.34N 77.61E W | Opportunity 1.95S 354.47E E
Sol 7 20:30: Curiosity 4.99S 137.45E S | Perseverance 18.34N 77.36E W | Opportunity 1.95S 354.47E E
```

With the `--map` option, a map of the relief around each rover is drawn after the results, north up: each cell is a character from the lowest (`.`) to the highest (`@`) elevation of the window. Each rover has its own letter (the first letter of its name that no rover before it uses, or else a free letter, then a digit: up to 36 rovers), in capitals where it is, in lower case where it landed, and `~` marks the cells the rovers drove through. Around Curiosity, the floor of Gale crater appears as dots:

```
Around Curiosity (C Curiosity, P Perseverance; lower case where they landed, ~ where they drove):
Relief from -4511 m (.) to 1165 m (@), north up, 31 x 15 cells:
=+++**++=--=====--=++*++++=****
+*+****++====--=====+#*##**+**+
+++***++**+=:....-+=+++++++===*
++*******+=-.c.:..:+*=-==+===-=
=+*******+-..~+#=..+*+====+==-=
*###*****+-..~+%+=:=*++++=**+--
##+#******-:-~~C==-+##*******=-
```

The option is ignored, with a warning, for rovers on a plateau.

**Small planets in text.** `planet map mini.txt` reads a planet from a text file next to the mission: its radius, the length of its day (its sol, in which its durations are given), the height of one step, then one row per line, from north to south, one digit per cell (the elevation in steps). Every row has the same, even, number of cells:

```
radius-km 10
day-hours 24.6597
meters-per-digit 1000
00000000
00090000
```

**Simplifications.** At 4 cells per degree, slopes are smoothed over about 15 km, which is why the default slope limit is low. A move goes from a cell to the next one, and its distance is the one between their centers. The rover keeps the place where it touched down inside its cell: its exact position makes the same step as its cell at each move. Across a pole, it goes on to the other side, so a rover north of the center of its cell ends south of the center of the next one. The orbit is taken as a circle: the seasons go at a steady pace, whereas the eccentric orbit of Mars makes its southern summers shorter and hotter. The power of a rover is computed every 10 minutes, where the rover is along its way between the centers of two cells; its consumption does not depend on the slope or the temperature, and dust does not settle on its panels.

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
java -jar target/rover.jar src/main/resources/mars-mission.txt
java -jar target/rover.jar --history --map src/main/resources/mars-mission.txt
```

## Tests

```bash
mvn test
```

The tests read input files from `src/test/resources/simulations/`:

- `valid/<name>.txt`: input of a nominal case, run through the program and compared to the expected result in `<name>.expected.txt` (`MainTest`);
- `invalid/<name>.txt`: input of an error case, used to check that errors are reported (`TextFileReaderTest`, `MainTest`).

To add a nominal case, create both files and add the name to `MainTest.validFile`. Format errors are tested directly on strings in `TextFormatParserTest` and `MissionParserTest`. `mini-planet.txt` is a mission on the small planet drawn in `mini-planet.map.txt`; `mars.txt` is a mission on the real Mars.

`ArchitectureTest` (ArchUnit) checks the direction of the dependencies between packages: `domain` depends on nothing else; `domain.simulation` and `mars.planet` only on `domain`, and never on each other; `mars.rover` on `mars.planet`; `mars.mission` on all these; `input` on all of them; and only the root package uses everything. The build fails if a dependency goes the wrong way.

## Structure

All classes are in the `fr.paulm.projects.rover` package.

`domain` holds the model (how a rover moves on a surface) and does not depend on the rest. `domain.simulation` uses it to run several rovers together and report on them. `mars.planet` describes a planet and its relief, `mars.rover` how the rovers drive on it, and `mars.mission` lands them. `input` reads a text file: rovers on a plateau, or a mission to a planet.

| Package | Class | Role |
|---|---|---|
| *(root)* | `Main` | entry point: reads the arguments and the file, runs the simulation, handles errors |
| | `CommandLine`, `Mode` | the arguments: the simulation mode, the `--history` and `--map` options and the file; each mode with the option that selects it |
| | `ConsoleReport`, `MissionReport` | print the results on the standard output, and the warnings through the logger: the history and the final positions on a plateau; the landings, the history, the end of each mission and the maps on a planet |
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
| | `Orbit`, `Sun` | the year, the tilt of the axis and the sunlight of a planet; the season, the local time, the height of the Sun and the sunlight on the ground |
| `mars.rover` | `ReliefTerrain`, `Mobility` | the slopes between cells, which ones a rover can drive and how long it takes; the speed and slope limit of the rovers |
| | `PowerSource`, `PowerSystem` | a radioisotope generator or solar panels; the source and the battery of a rover, and what it uses |
| `mars.mission` | `PlanetMission`, `RoverPlan` | the planet, the settings, the seed and the plan of each rover (name, landing site, heading, orders) |
| | `Landing`, `LandingRules`, `LandingSite`, `Ellipse` | checks a landing site and draws the touchdown point in its ellipse |
| | `Sky`, `LandingDate` | the season and the time at the start of the mission, and the dust in the atmosphere; the sol and local time a rover lands at |
| | `Deployment`, `Touchdown`, `LandingFailure` | the rovers on the ground: where each one landed, which ones are lost, and the simulation input of the others |
| | `Batteries` | the batteries of the rovers through the mission: the pace that makes them wait to recharge, or stop |
| | `RoverPositions` | follows the exact position of each rover that landed, turn by turn, across the poles too |
| `input` | `SimulationReader`, `Scenario` | interface: a possible input for the simulation (text file, and later JSON, database…); what it describes, rovers on a plateau or a mission to a planet |
| | `TextFileReader` | reads a file and passes its content to the parser of its format |
| | `TextFormatParser` | reads and validates the plateau format, wherever the content comes from; errors give the line number |
| | `MissionParser`, `TextMapParser` | read and validate the mission format and the small planets drawn in text |
| | `MolaFileReader` | reads the MOLA topography of Mars |
