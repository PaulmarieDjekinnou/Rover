# Telemetry with Kafka

[Back to the README](../README.md)

With the `--telemetry server:port` option, a mission to a planet also sends its telemetry to Kafka, as the rovers would send it, in JSON:

| Topic | Key | Messages |
|---|---|---|
| `rover.telemetry` | the rover's name | one message each time a rover ends an instruction: mission time, position, cell, direction, battery, day or night, distance driven, dust opacity, cleanliness of the solar panels, and a sequence number per rover |
| `rover.events` | the rover's name | landed, lost at landing, no route to a waypoint, move blocked by the terrain or by a rover, dust storm started or ended where it is, panels cleaned by the wind, stopped for good; storms and cleanings are seen when the rover ends an instruction, so a storm that passes between two messages is not reported |
| `rover.plans` | the rover's name | the plan of each rover, at the start: its site, power source, battery, landing time and number of waypoints |
| `mission.status` | the mission id | the start and the end of the mission (file, planet, length of its sol, seed) |

Each run has its own mission id. All the messages of a rover share its key, so they go to the same partition and stay in order. The producer waits for the broker to confirm each message (`acks=all`) and never doubles one when it retries (idempotence). Without the option, nothing is sent; on a plateau, the option is ignored with a warning. If Kafka does not answer within 5 seconds, the program says so and stops before the mission; a message that cannot be sent during the mission is counted, and a warning at the end gives how many were lost. With `--telemetry file:mission.jsonl`, the messages are written to a file instead, one per line (JSON Lines), each with its topic and key: to look at a mission or compare two runs without Kafka.

**The link to Earth.** When the mission has a `start-date`, the messages of the rovers do not reach Earth at once. Each rover keeps them in its memory until a relay orbiter passes over it (`relay-passes`), and a pass carries only so many (`pass-messages`); they then take the light delay to reach Earth, from about 3 to 22 minutes depending on where the two planets are, and nothing gets through while Mars is within 2 degrees of the Sun seen from Earth (a solar conjunction, about two weeks every 26 months). A full memory (`rover-memory`) loses its oldest telemetry first, then its oldest events: the gap shows in the sequence numbers, and a warning at the end gives how many were lost. Each telemetry message and event carries both times: `missionSeconds`, when the rover produced it, and `receivedSeconds`, when it reached Earth; the messages are sent in the order they arrive. The plans and the mission status are written on Earth, so they go straight through. Without a `start-date`, both times are the same.

**The ground station** is a second application, in its own module and jar: it reads the three topics (consumer group `ground-station`) and archives the messages in PostgreSQL (tables `missions`, `rovers`, `telemetry` and `events`). It creates and upgrades its schema itself when it starts (Flyway, `ground-station/src/main/resources/db/migration`). It commits its Kafka offsets only once the messages are in the database: after a crash, it reads them again, and a message read twice is written once (telemetry and events are numbered per rover). A message it cannot read does not stop it: it goes to the `rover.dead-letters` topic, with the reason in its headers.

```bash
docker compose up -d     # Kafka (KRaft, with its topics) and PostgreSQL
java -jar simulator/target/rover.jar --telemetry localhost:9092 simulator/src/main/resources/mars-mission.txt
java -jar ground-station/target/ground-station.jar localhost:9092 jdbc:postgresql://localhost:5432/rover --until-idle 10
```

The database user and password come from `ROVER_DB_USER` and `ROVER_DB_PASSWORD` (`rover` by default).

**Mission control** follows the rovers of every mission live (the most recent mission first), in the console and, with `--web 8080`, on a web page at `http://localhost:8080` (a table of the rovers and a map of their positions, refreshed every second), from Kafka: the last known position, direction, battery and day or night of each rover, the dust, and its status (active, stopped, silent when it has sent nothing for a sol of its planet, done once the mission has ended). Each mission control has its own consumer group and starts from the latest messages, while the ground station archives everything in its own group:

```bash
java -cp ground-station/target/ground-station.jar fr.paulm.projects.rover.ground.MissionControl localhost:9092 --web 8080
java -jar simulator/target/rover.jar --speed 8877 --telemetry localhost:9092 simulator/src/main/resources/mars-mission.txt
```

**Live.** With `--speed factor`, a mission runs at that many seconds of mission per real second, so that it can be followed live, in the history or in Kafka: `--speed 8877` runs a sol in about 10 seconds. The simulation waits before each turn until its time has come; on a plateau, the option is ignored with a warning.

