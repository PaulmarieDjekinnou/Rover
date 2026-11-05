# Missions to Mars

[Back to the README](../README.md)

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
| `resolution 64` | optional: drive on a finer grid, in cells per degree (a multiple of 4 on Mars, up to 512): the relief of each cell is the map interpolated, plus a detail drawn from the seed; 64 gives cells of about 925 m instead of 15 km |
| `rocks 0.03` | optional: the share of cells covered by rock fields, too small for the orbital map: a rover with waypoints discovers them when it is next to them, and its AutoNav plans around them (none by default) |
| `max-slope 10` | optional: the steepest slope the rovers can drive on, in degrees (10 by default) |
| `speed 0.04` | optional: the speed of the rovers on flat ground, in m/s (0.04 by default, about Curiosity's top speed) |
| `turn-minutes 5` | optional: how long a turn on the spot takes, in minutes (5 by default) |
| `landing-max-elevation -1000` | optional: the highest landing site allowed, in meters (-1000 by default) |
| `landing-max-latitude 30` | optional: the farthest landing site from the equator, in degrees (30 by default) |
| `start-ls 150` | optional: the season at the landing, as a solar longitude Ls in degrees: 0 at the northern spring equinox, 90 at the northern summer solstice (0 by default) |
| `landing-time 15:00` | optional: the local solar time at the start of the mission, at the site of the first rover (12:00 by default) |
| `drive-window 10:00 15:00` | optional: the local solar times between which the rovers drive and turn (10:00 to 15:00 by default) |
| `max-drive-per-sol 50` | optional: the farthest a rover drives in a drive sol, in meters (50 by default) |
| `drive-every 3` | optional: the rovers drive one sol in that many, the others go to science (3 by default) |
| `dust-opacity 0.5` | optional: a dust opacity that stays the same all along: about 0.5 on a clear day, above 4 in a global dust storm (by default, the weather of Mars, see below) |
| `rover <name>` | starts the block of a rover |
| `site <latitude> <longitude>` | where the rover aims: latitude negative in the south, longitude from 0 to 360 to the east |
| `ellipse <major> <minor> <azimuth>` | optional: the landing ellipse, axes in km, azimuth of the major axis in degrees from north (none by default) |
| `heading N` | the direction the rover faces once on the ground |
| `orders MMRM` | optional: the instructions, as on the plateau |
| `power rtg 110` | optional: the power source, a radioisotope generator of 110 W (like Curiosity's), or `solar 1.3` for 1.3 m2 of solar panels (like Opportunity's) (`rtg 110` by default) |
| `battery 2000` | optional: the capacity of the battery, in Wh (2000 by default) |
| `drive-window`, `max-drive-per-sol`, `drive-every` | optional, in a rover block: the operations of this rover, if they differ from the mission's |
| `goto -4.90 137.40` | instead of `orders`: a waypoint to reach, one line per waypoint, in order; the route is planned after the landing |
| `landing 3 14:00` | optional: the rover lands on sol 3 of the mission, when it is 14:00 local time at its site (by default, at the start of the mission, with the first rover) |

Lines starting with `#` are comments. The whole file is checked before anything lands, and errors give the line number.

**The planet.** `planet mars` uses the topography measured by the MOLA laser altimeter of Mars Global Surveyor (NASA, public domain), at 4 cells per degree: 1440 × 720 cells of about 15 km at the equator, in `simulator/src/main/resources/mars/` with the description of its format (`.lbl`). The planet is a sphere cut into a latitude / longitude grid: `x` is the column (the longitude, to the east) and `y` the row (the latitude, from the south pole). Going east or west wraps around the planet; going north from the last row crosses the north pole: the rover arrives on the same row, half a turn of longitude away, now facing south (and likewise at the south pole).

**The landing.** A site is refused if it is too high (the parachute needs enough atmosphere), too far from the equator, or next to a slope the rover could not drive. Each rover then touches down at a random point of its ellipse. A rover that touches down less than 100 m from another rover, or in a cell it cannot leave, is lost: it takes no part in the simulation.

**The drive.** A move is blocked by the terrain when the slope between the two cells is steeper than `max-slope`, uphill or downhill. Distances are measured in meters between the centers of the cells, along the surface; climbing slows the rover down, to half its speed at the steepest slope. Unlike on the plateau, several rovers can be in the same cell: a cell is about 15 km wide, so they are kilometers apart, and rovers never block each other. Each instruction takes its time: a move lasts its distance divided by the speed, a turn on the spot `turn-minutes` (5 minutes by default), and a move blocked by the terrain no time. A rover starts its next instruction as soon as the previous one is over, and all the rovers share the same clock: the simulation handles the ends of instructions in time order. The three modes of the plateau do not apply to a mission: their options are ignored, with a warning. On a plateau, where instructions take no time, this clock would give exactly the step-by-step mode.

**The Sun.** The planet turns on itself once a sol and goes around the Sun once a year (668.6 sols on Mars), on an axis tilted by 25.19 degrees, which makes the seasons. Its orbit is eccentric (0.0934): Mars goes faster near the Sun (Kepler's equation gives where it is), so the seasons do not last the same time (the northern spring about 194 sols, the northern autumn about 142), and sunlight above the atmosphere goes from about 490 W/m2 at the aphelion to 713 at the perihelion (Ls 251). From the season (Ls), the time and the place, the simulation knows the local solar time and how high the Sun is. Sunlight on the ground is weakened by the dust, all the more as the Sun is low.

**The weather.** Unless `dust-opacity` fixes it, the dust follows the weather of Mars: clear skies in the northern spring and summer, twice as much dust at the height of the dust season (Ls 270), two regional storms a year in that season, and one year in three a global storm that lasts for months (opacity above 4), as in 2001, 2007 and 2018. The storms are drawn from the seed. Solar panels gather dust (0.15 % of their power a sol, three times faster in a storm) until the wind cleans them (about once in 60 sols, giving back 30 %); the results give how clean they are. These rates are orders of magnitude, not a fit to the missions.

**The waypoints.** Instead of orders, a rover can be given waypoints (`goto`), as real missions give destinations. Once it has landed, its route is planned on the orbital map with an A* search: the states are a cell and a direction (turning takes time), a step is a turn or a move the terrain allows, its cost is the energy it takes (its time, times the extra power of climbing), and the estimate of the cost left is the distance as the crow flies at the speed on flat ground, so the route is the cheapest one in energy: it may go around a hill rather than over it. The route becomes ordinary instructions (L, R, M). A waypoint no route reaches is skipped, and the next ones are planned from where the rover then is; the results say which were missed (`route to 2 of 3 waypoints, none to 5.08S 137.85E`), and the telemetry sends a `NO_ROUTE` event for each. On a planet with rock fields, the rover drives with its AutoNav, as Perseverance does: before each move it looks at the cell ahead, remembers a rock field it finds there, and plans the rest of the way again around the rock fields it knows; the results say how many it found.

**The operations.** Like real missions, the rovers are operated sol by sol: a rover drives only one sol in `drive-every` (the other sols go to science, the rover standing), only in its drive window, at its local time, and at most `max-drive-per-sol` meters in a drive sol; then it waits. Real rovers average some 8 to 23 m per sol over their missions (Curiosity about 32 km by sol 4000, Perseverance 45.2 km by sol 2006, Opportunity 45.2 km by sol 5111) and rarely drive more than a few hundred meters in a sol (Perseverance's record: 411.7 m): `RealMissionsTest` checks each rover of the sample mission within a factor of 3 of its real model.

**The energy.** Each rover has a battery, full at the landing, charged by its power source: a generator gives the same power day and night, solar panels turn a fifth of the sunlight they get into power, so nothing at night, little in winter or in a dust storm. A rover uses 150 W when it drives on flat ground or turns, up to twice as much when it climbs the steepest slope it can take, and 15 W when it waits. At night, a solar rover also powers its electric survival heaters, from 5 W to 20 W (most of the heat of Spirit and Opportunity came from radioisotope heater units), more when the Sun stays low at noon (winter, high latitude); a rover with a radioisotope generator is kept warm by it. It works while its battery holds more than a fifth of its capacity; below, it waits and recharges until the battery is four fifths full, then goes on, so a solar rover drives by day and waits at night. Waiting takes time on the common clock. A rover whose battery does not recharge in a whole year of waiting stops for good, like Spirit in 2010: its other orders are left undone.

Each rover enters the simulation when it lands, on the common clock: rovers sent at different dates land on different sols. The output gives the landing of each rover, on the mission clock (the sol, from 0, and the time of day), then, for each rover that landed, its final position, its cell and direction, the distance driven and the time spent driving, the time spent waiting (for energy, for the drive window, or for the next sol), in sols (Martian days of 24 h 39 min 35 s), and the charge of its battery, on several lines per rover. Both give the local time and the Sun:

```
Curiosity landed on Sol 0 00:00 at 4.74S 137.45E (cell 549 341), 9.1 km from its target
  local time 15:00, Sun 42 degrees high
Perseverance landed on Sol 2 04:52 at 18.34N 77.61E (cell 310 433), 3.0 km from its target
  local time 15:53, Sun 34 degrees high
Opportunity landed on Sol 5 07:46 at 1.95S 354.47E (cell 1417 352), 0.0 km from its target
  local time 13:15, Sun 67 degrees high
Curiosity: 5.24S 137.95E (cell 551 339) facing E
  route to 2 of 2 waypoints
  59.0 km driven in 21.3 sols, 3521.5 sols waiting
  battery 99%, local time 10:13, Sun 58 degrees high
Perseverance: 18.84N 76.86E (cell 307 435) facing N
  71.7 km driven in 21.0 sols, 4278.8 sols waiting
  battery 99%, local time 10:14, Sun 49 degrees high
Opportunity: 1.95S 354.97E (cell 1419 352) facing E
  29.6 km driven in 8.3 sols, 4630.5 sols waiting
  battery 41%, panels 93% clean, local time 10:09, Sun 56 degrees high
```

A rover that ran out of energy gets one more line, with the time it stopped: `stopped on Sol 668 17:39, out of energy`. The instruction it was on is not done: it stays in its cell, but the time and the distance it drove before giving up count. Warnings name the rovers: `Climber: 1 move(s) blocked by the terrain.` With `--history`, a line is printed each time an instruction ends, in time order: the time of the common clock, as the sol since the start of the mission (from 0) and the time of day (a sol cut into 24 hours, like the local time on Mars), then each rover that has landed by then, with its position, direction and battery charge, `(night)` where the Sun is down, and the names of the blocked rovers:

```
Sol 7 09:51: Curiosity 4.99S 137.45E S 80% (night) | Perseverance 18.34N 77.61E W 30% (night) | Opportunity 1.95S 354.47E E 50%
Sol 7 20:30: Curiosity 4.99S 137.45E S 58% | Perseverance 18.34N 77.36E W 80% | Opportunity 1.95S 354.47E E 0% (night)
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

**Simplifications.** At 4 cells per degree, slopes are smoothed over about 15 km, which is why the default slope limit is low. A move goes from a cell to the next one, and its distance is the one between their centers. The rover keeps the place where it touched down inside its cell: its exact position makes the same step as its cell at each move. Across a pole, it goes on to the other side, so a rover north of the center of its cell ends south of the center of the next one. The power of a rover is computed every 10 minutes while it works, every hour while it waits (a step never jumps over the start of a drive window), where the rover is along its way between the centers of two cells; its panels lie flat.

